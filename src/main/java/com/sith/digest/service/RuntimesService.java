package com.sith.digest.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuntimesService {
    public List<String> getAllRuntimes(){
        return List.of("python","java");
    }
}
