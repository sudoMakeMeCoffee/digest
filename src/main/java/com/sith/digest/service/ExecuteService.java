package com.sith.digest.service;

import com.sith.digest.dto.request.ExecuteRequestDto;
import com.sith.digest.dto.request.FileDto;
import com.sith.digest.dto.response.ExecuteResponseDto;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ExecuteService {

    public ExecuteResponseDto execute(ExecuteRequestDto requestDto) throws IOException, InterruptedException {
        // Create temp dir for this execution
        Path tempDir = Files.createTempDirectory("docker-runner-");

        // Prepare variables
        String image;
        String compileCommand = null;
        String runCommand;
        List<String> args = requestDto.getArgs() != null ? requestDto.getArgs() : Collections.emptyList();
        long runTimeoutMs = Optional.ofNullable(requestDto.getRunTimeout()).orElse(10000L);
        long compileTimeoutMs = Optional.ofNullable(requestDto.getCompileTimeout()).orElse(5000L);

        // Write all files to tempDir
        for (FileDto file : requestDto.getFiles()) {
            String fileName = file.getName() != null ? file.getName() : UUID.randomUUID().toString();
            Path filePath = tempDir.resolve(fileName);
            byte[] contentBytes;
            switch (Optional.ofNullable(file.getEncoding()).orElse("utf8").toLowerCase()) {
                case "base64":
                    contentBytes = Base64.getDecoder().decode(file.getContent());
                    break;
                case "hex":
                    contentBytes = hexStringToByteArray(file.getContent());
                    break;
                case "utf8":
                default:
                    contentBytes = file.getContent().getBytes();
                    break;
            }
            Files.write(filePath, contentBytes);
        }

        // Decide Docker image and commands based on language
        switch (requestDto.getLanguage().toLowerCase()) {
            case "java":
                image = "openjdk:21-slim";
                compileCommand = "javac /code/Main.java";
                runCommand = "java -cp /code Main " + String.join(" ", args);
                break;
            case "python":
                image = "python:3.11-slim";
                runCommand = "python /code/" + getFirstFileName(requestDto) + " " + String.join(" ", args);
                break;
            case "csharp":
            case "c#":
                image = "mcr.microsoft.com/dotnet/sdk:8.0";
                compileCommand = "dotnet new console -o /code/app && mv /code/Program.cs /code/app/Program.cs";
                runCommand = "cd /code/app && dotnet run -- " + String.join(" ", args);
                break;
            case "cpp":
            case "c++":
                image = "gcc:13.2";
                compileCommand = "g++ /code/" + getFirstFileName(requestDto) + " -o /code/a.out";
                runCommand = "/code/a.out " + String.join(" ", args);
                break;
            default:
                throw new IllegalArgumentException("Unsupported language: " + requestDto.getLanguage());
        }

        // Build the full container command
        String containerCommand;
        if (compileCommand != null) {
            containerCommand = String.format("bash -c \"%s && %s\"", compileCommand, runCommand);
        } else {
            containerCommand = runCommand;
        }

        // Build docker command
        List<String> dockerCommand = new ArrayList<>(Arrays.asList(
                "docker", "run", "--rm",
                "--cpus=0.5",
                "--memory=512m",
                "--network=none",
                "-v", tempDir.toAbsolutePath() + ":/code",
                image,
                "bash", "-c", containerCommand
        ));

        ProcessBuilder pb = new ProcessBuilder(dockerCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Write stdin if provided
        if (requestDto.getStdin() != null && !requestDto.getStdin().isEmpty()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(requestDto.getStdin());
                writer.flush();
            }
        }

        // Wait for completion with timeout
        boolean finished = process.waitFor(runTimeoutMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            cleanup(tempDir);
            return new ExecuteResponseDto("Execution timed out.", "", 124);
        }

        // Collect output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.exitValue();
        cleanup(tempDir);

        ExecuteResponseDto response = new ExecuteResponseDto();
        response.setStdout(output.toString());
        response.setStderr(""); // combined output
        response.setExitCode(exitCode);

        return response;
    }

    private String getFirstFileName(ExecuteRequestDto requestDto) {
        if (requestDto.getFiles() != null && !requestDto.getFiles().isEmpty()) {
            FileDto first = requestDto.getFiles().get(0);
            return first.getName() != null ? first.getName() : "main.py";
        }
        return "main.py"; // fallback
    }

    private void cleanup(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for(int i = 0; i < len; i += 2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i),16) << 4)
                    + Character.digit(s.charAt(i+1),16));
        }
        return data;
    }
}
