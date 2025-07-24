package com.sith.digest.service;

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

        String image;
        String containerCommand;
        String mainFileName = null;

        String language = requestDto.getLanguage().toLowerCase();
        String version = requestDto.getVersion() != null ? requestDto.getVersion() : getDefaultVersion(language);

        // Write all provided files
        for (FileDto file : requestDto.getFiles()) {
            String name = file.getName() != null ? file.getName() : UUID.randomUUID().toString();
            Path filePath = tempDir.resolve(name);
            Files.writeString(filePath, file.getContent());
            if (mainFileName == null) mainFileName = name;
        }

        // Determine Docker image and command
        switch (language) {
            case "python":
                image = "python:" + version + "-slim";
                containerCommand = "python /code/" + mainFileName;
                break;

            case "java":
                image = "openjdk:" + version + "-slim";
                containerCommand = "bash -c \"javac /code/" + mainFileName + " && java -cp /code Main\"";
                break;

            case "csharp":
            case "c#":
                image = "mcr.microsoft.com/dotnet/sdk:" + version;
                containerCommand = "bash -c \"dotnet new console -o /code/app && mv /code/*.cs /code/app/ && cd /code/app && dotnet run\"";
                break;

            case "cpp":
            case "c++":
                image = "gcc:" + version;
                containerCommand = "bash -c \"g++ /code/" + mainFileName + " -o /code/a.out && /code/a.out\"";
                break;

            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }

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

        // Write to stdin if provided
        if (requestDto.getStdin() != null && !requestDto.getStdin().isBlank()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(requestDto.getStdin());
                writer.flush();
            }
        }

        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
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
        result.stderr = ""; // merged with stdout
        result.exitCode = exitCode;

        return result;
    }

    private String getDefaultVersion(String language) {
        return switch (language) {
            case "python" -> "3.11";
            case "java" -> "21";
            case "csharp", "c#" -> "8.0";
            case "cpp", "c++" -> "13.2";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }
}
