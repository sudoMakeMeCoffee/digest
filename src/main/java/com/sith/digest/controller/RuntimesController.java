package com.sith.digest.controller;

import com.sith.digest.service.RuntimesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/runtimes")
public class RuntimesController {

    private final RuntimesService runtimesService;

    public RuntimesController(RuntimesService runtimesService) {
        this.runtimesService = runtimesService;
    }

    @GetMapping
    public List<String> getAllRuntimes(){
        return runtimesService.getAllRuntimes();
    }
}
