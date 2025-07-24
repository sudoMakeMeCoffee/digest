package com.sith.digest.config;

import java.util.Locale;

public enum LanguageConfig {

    PYTHON("python", "3.11", version -> "python:" + version + "-slim",
            (file) -> "python /code/" + file),

    JAVA("java", "21", version -> "openjdk:" + version + "-slim",
            (file) -> "bash -c \"javac /code/" + file + " && java -cp /code Main\""),

    CSHARP("c#", "8.0", version -> "mcr.microsoft.com/dotnet/sdk:" + version,
            (file) -> "bash -c \"dotnet new console -o /code/app && mv /code/*.cs /code/app/ && cd /code/app && dotnet run\""),

    CPP("cpp", "13.2", version -> "gcc:" + version,
            (file) -> "bash -c \"g++ /code/" + file + " -o /code/a.out && /code/a.out\"");

    private final String key;
    private final String defaultVersion;
    private final DockerImageResolver imageResolver;
    private final DockerCommandResolver commandResolver;

    LanguageConfig(String key, String defaultVersion,
                   DockerImageResolver imageResolver,
                   DockerCommandResolver commandResolver) {
        this.key = key;
        this.defaultVersion = defaultVersion;
        this.imageResolver = imageResolver;
        this.commandResolver = commandResolver;
    }

    public String getImage(String version) {
        return imageResolver.resolve(version);
    }

    public String getCommand(String mainFile) {
        return commandResolver.resolve(mainFile);
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public static LanguageConfig from(String language) {
        return switch (language.toLowerCase(Locale.ROOT)) {
            case "python" -> PYTHON;
            case "java" -> JAVA;
            case "c#", "csharp" -> CSHARP;
            case "cpp", "c++" -> CPP;
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    @FunctionalInterface
    interface DockerImageResolver {
        String resolve(String version);
    }

    @FunctionalInterface
    interface DockerCommandResolver {
        String resolve(String mainFile);
    }
}
