package com.sith.digest.dto.request;

import java.util.List;

public class ExecuteRequestDto {
    private String language;
    private String version;
    private List<FileDto> files;
    private String stdin;
    private List<String> args;
    private Long runTimeout;         // in milliseconds
    private Long compileTimeout;     // in milliseconds
    private Long compileMemoryLimit; // in bytes
    private Long runMemoryLimit;     // in bytes

    // Getters and setters

    public String getLanguage() {
        return language;
    }
    public void setLanguage(String language) {
        this.language = language;
    }

    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }

    public List<FileDto> getFiles() {
        return files;
    }
    public void setFiles(List<FileDto> files) {
        this.files = files;
    }

    public String getStdin() {
        return stdin;
    }
    public void setStdin(String stdin) {
        this.stdin = stdin;
    }

    public List<String> getArgs() {
        return args;
    }
    public void setArgs(List<String> args) {
        this.args = args;
    }

    public Long getRunTimeout() {
        return runTimeout;
    }
    public void setRunTimeout(Long runTimeout) {
        this.runTimeout = runTimeout;
    }

    public Long getCompileTimeout() {
        return compileTimeout;
    }
    public void setCompileTimeout(Long compileTimeout) {
        this.compileTimeout = compileTimeout;
    }

    public Long getCompileMemoryLimit() {
        return compileMemoryLimit;
    }
    public void setCompileMemoryLimit(Long compileMemoryLimit) {
        this.compileMemoryLimit = compileMemoryLimit;
    }

    public Long getRunMemoryLimit() {
        return runMemoryLimit;
    }
    public void setRunMemoryLimit(Long runMemoryLimit) {
        this.runMemoryLimit = runMemoryLimit;
    }
}
