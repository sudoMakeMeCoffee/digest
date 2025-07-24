package com.sith.digest.service;

import com.sith.digest.config.LanguageConfig;
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

    private static final long DEFAULT_RUN_TIMEOUT_MS = 5000L; // default 5 seconds

    public ExecuteResponseDto execute(ExecuteRequestDto requestDto) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("docker-runner-");

        String mainFileName = writeSourceFiles(requestDto.getFiles(), tempDir);

        LanguageConfig langConfig = LanguageConfig.from(requestDto.getLanguage());
        String version = Optional.ofNullable(requestDto.getVersion()).orElse(langConfig.getDefaultVersion());
        String image = langConfig.getImage(version);

        // Build container command and append args if present
        String containerCommand = langConfig.getCommand(mainFileName);
        if (requestDto.getArgs() != null && !requestDto.getArgs().isEmpty()) {
            String joinedArgs = String.join(" ", requestDto.getArgs());
            containerCommand += " " + joinedArgs;
        }

        List<String> dockerCommand = List.of(
                "docker", "run", "--rm",
                "-v", tempDir.toAbsolutePath() + ":/code",
                image,
                "bash", "-c", containerCommand
        );

        ProcessBuilder pb = new ProcessBuilder(dockerCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        writeStdinToProcess(process, requestDto.getStdin());

        long timeoutMs = Optional.ofNullable(requestDto.getRunTimeout()).orElse(DEFAULT_RUN_TIMEOUT_MS);
        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

        if (!finished) {
            process.destroyForcibly();
            cleanupTempDirectory(tempDir);

            ExecuteResponseDto timeoutResult = new ExecuteResponseDto();
            timeoutResult.stdout = "";
            timeoutResult.stderr = "Error: Execution timed out after " + timeoutMs + " ms";
            timeoutResult.exitCode = -1;
            return timeoutResult;
        }

        String output = readProcessOutput(process);
        int exitCode = process.exitValue();

        cleanupTempDirectory(tempDir);

        ExecuteResponseDto result = new ExecuteResponseDto();
        result.stdout = output;
        result.stderr = ""; // merged with stdout
        result.exitCode = exitCode;
        return result;
    }

    private String writeSourceFiles(List<FileDto> files, Path tempDir) throws IOException {
        String mainFileName = null;
        for (FileDto file : files) {
            String name = Optional.ofNullable(file.getName()).orElse(UUID.randomUUID().toString());
            Path filePath = tempDir.resolve(name);
            Files.writeString(filePath, file.getContent());
            if (mainFileName == null) mainFileName = name;
        }
        return mainFileName;
    }

    private void writeStdinToProcess(Process process, String stdin) throws IOException {
        if (stdin != null && !stdin.isBlank()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(stdin);
                writer.flush();
            }
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    private void cleanupTempDirectory(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace(); // replace with proper logging if needed
        }
    }
}
