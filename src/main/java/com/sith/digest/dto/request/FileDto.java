package com.sith.digest.dto.request;

public class FileDto {
    private String name;
    private String content;
    private String encoding;  // base64, hex, utf8 (default utf8)

    // Getters and setters

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getEncoding() {
        return encoding;
    }
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
