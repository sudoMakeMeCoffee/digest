package com.sith.digest.controller;

import com.sith.digest.dto.request.ExecuteRequestDto;
import com.sith.digest.dto.response.ExecuteResponseDto;
import com.sith.digest.service.ExecuteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/execute")
public class ExecuteController {

    private final ExecuteService executeService;

    public ExecuteController(ExecuteService executeService) {
        this.executeService = executeService;
    }

    @PostMapping
    public ExecuteResponseDto execute(@RequestBody ExecuteRequestDto requestDto) throws IOException, InterruptedException {
        return executeService.execute(requestDto);
    }
}
