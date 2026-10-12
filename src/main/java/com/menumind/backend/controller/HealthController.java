package com.menumind.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health Controller
 * 
 * Provides a simple sanity-check REST endpoint to verify that the
 * Spring Boot application server is running and accessible.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("project", "MenuMind AI Backend");
        response.put("message", "Spring Boot Backend initialized successfully!");
        response.put("phase", "Phase 1: Project Setup");
        return ResponseEntity.ok(response);
    }
}
