package com.sith.digest.service;

import com.sith.digest.dto.request.ExecuteRequestDto;
import com.sith.digest.dto.response.ExecuteResponseDto;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ExecuteService {

    private static final int TIMEOUT_SECONDS = 10; // Timeout per execution

    public ExecuteResponseDto execute(ExecuteRequestDto requestDto) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("docker-runner-");
        String fileName;
        String image;
        String containerCommand;
        String memoryLimit = "256m";
        String cpuLimit = "0.5";

        switch (requestDto.getLanguage().toLowerCase()) {
            case "python":
                fileName = "main.py";
                image = "python:3.11-slim";
                containerCommand = "python /code/" + fileName;
                break;

            case "java":
                fileName = "Main.java";
                image = "openjdk:21-slim";
                containerCommand = "bash -c \"javac /code/Main.java && java -cp /code Main\"";
                memoryLimit = "512m";
                break;

            case "csharp":
            case "c#":
                fileName = "Program.cs";
                image = "mcr.microsoft.com/dotnet/sdk:8.0";
                containerCommand = "bash -c \"dotnet new console -o /code/app && mv /code/Program.cs /code/app/Program.cs && cd /code/app && dotnet run\"";
                memoryLimit = "512m";
                break;

            case "cpp":
            case "c++":
                fileName = "main.cpp";
                image = "gcc:13.2";
                containerCommand = "bash -c \"g++ /code/main.cpp -o /code/a.out && /code/a.out\"";
                break;

            default:
                throw new IllegalArgumentException("Unsupported language: " + requestDto.getLanguage());
        }

        // Write code to file
        Path filePath = tempDir.resolve(fileName);
        Files.writeString(filePath, requestDto.getCode());

        // Docker command with resource limits and timeout
        List<String> dockerCommand = List.of(
                "docker", "run", "--rm",
                "--cpus=" + cpuLimit,
                "--memory=" + memoryLimit,
                "--network=none",
                "-v", tempDir.toAbsolutePath() + ":/code",
                image,
                "bash", "-c", containerCommand
        );

        ProcessBuilder pb = new ProcessBuilder(dockerCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly(); // force kill
            cleanup(tempDir);
            return new ExecuteResponseDto("Execution timed out.", "", 124); // 124 = timeout convention
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.exitValue();
        cleanup(tempDir);

        ExecuteResponseDto result = new ExecuteResponseDto();
        result.stdout = output.toString();
        result.stderr = ""; // already redirected
        result.exitCode = exitCode;

        return result;
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
}
