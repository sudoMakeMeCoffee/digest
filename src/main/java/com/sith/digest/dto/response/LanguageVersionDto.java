package com.sith.digest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LanguageVersionDto {
    private String language;
    private String defaultVersion;

    public LanguageVersionDto(String language, String defaultVersion) {
        this.language = language;
        this.defaultVersion = defaultVersion;
    }

}