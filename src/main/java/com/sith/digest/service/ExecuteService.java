package com.sith.digest.service;

import com.sith.digest.dto.request.ExecuteRequestDto;
import com.sith.digest.dto.response.ExecuteResponseDto;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Service
public class ExecuteService {
    public ExecuteResponseDto execute(ExecuteRequestDto requestDto) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("docker-runner-");
        String fileName;
        String image;
        String containerCommand;

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
                break;

            default:
                throw new IllegalArgumentException("Unsupported language: " + requestDto.getLanguage());
        }

        // Write code to file
        Path filePath = tempDir.resolve(fileName);
        Files.writeString(filePath, requestDto.getCode());

        // Build Docker command
        List<String> dockerCommand = List.of(
                "docker", "run", "--rm",
                "-v", tempDir.toAbsolutePath() + ":/code",
                image,
                "bash", "-c", containerCommand
        );

        ProcessBuilder pb = new ProcessBuilder(dockerCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();

        // Cleanup
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ExecuteResponseDto result = new ExecuteResponseDto();
        result.stdout = output.toString();
        result.stderr = ""; // merged into stdout
        result.exitCode = exitCode;

        return result;
    }
}
