package com.sith.digest.service;

import com.sith.digest.config.LanguageConfig;
import com.sith.digest.dto.request.ExecuteRequestDto;
import com.sith.digest.dto.request.FileDto;
import com.sith.digest.dto.response.ExecuteResponseDto;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ExecuteService {

    public ExecuteResponseDto execute(ExecuteRequestDto requestDto) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("docker-runner-");

        String mainFileName = null;

        // Write uploaded files to temp dir
        for (FileDto file : requestDto.getFiles()) {
            String name = file.getName() != null ? file.getName() : UUID.randomUUID().toString();
            Path filePath = tempDir.resolve(name);
            Files.writeString(filePath, file.getContent());
            if (mainFileName == null) mainFileName = name;
        }

        // Get language config
        LanguageConfig langConfig = LanguageConfig.from(requestDto.getLanguage());
        String version = requestDto.getVersion() != null ? requestDto.getVersion() : langConfig.getDefaultVersion();

        // Build Docker image and command
        String image = langConfig.getImage(version);
        String containerCommand = langConfig.getCommand(mainFileName);

        List<String> dockerCommand = List.of(
                "docker", "run", "--rm",
                "-v", tempDir.toAbsolutePath() + ":/code",
                image,
                "bash", "-c", containerCommand
        );

        // Run Docker process
        ProcessBuilder pb = new ProcessBuilder(dockerCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Write to stdin if provided
        if (requestDto.getStdin() != null && !requestDto.getStdin().isBlank()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(requestDto.getStdin());
                writer.flush();
            }
        }

        // Capture output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();

        // Cleanup temp dir
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Build response
        ExecuteResponseDto result = new ExecuteResponseDto();
        result.stdout = output.toString();
        result.stderr = ""; // already merged with stdout
        result.exitCode = exitCode;

        return result;
    }
}
