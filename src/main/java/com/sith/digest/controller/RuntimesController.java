package com.sith.digest.controller;

import com.sith.digest.config.LanguageConfig;
import com.sith.digest.dto.response.LanguageVersionDto;
import com.sith.digest.service.RuntimesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/runtimes")
public class RuntimesController {


    @GetMapping
    public List<LanguageVersionDto> getSupportedLanguages() {
        return Arrays.stream(LanguageConfig.values())
                .map(lang -> new LanguageVersionDto(lang.name().toLowerCase(), lang.getDefaultVersion()))
                .collect(Collectors.toList());
    }
}
