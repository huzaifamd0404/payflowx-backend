package com.payflowx.backend.controller;

import com.payflowx.backend.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        HealthResponse response = new HealthResponse(
            "UP",
            "PayFlowX Backend is running successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
}
