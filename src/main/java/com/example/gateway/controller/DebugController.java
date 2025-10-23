package com.example.gateway.controller;

import com.example.gateway.security.GoogleTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DebugController {

    @Autowired
    private GoogleTokenService googleTokenService;

    @GetMapping("/debug/service-account")
    public Map<String, Object> debugServiceAccount() {
        Map<String, Object> response = new HashMap<>();
        
        String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        response.put("GOOGLE_APPLICATION_CREDENTIALS", credentialsPath);
        
        if (credentialsPath != null) {
            java.io.File file = new java.io.File(credentialsPath);
            response.put("file_exists", file.exists());
            response.put("file_path", file.getAbsolutePath());
        }
        
        response.put("service_account_initialized", googleTokenService.isInitialized());
        
        if (googleTokenService.isInitialized()) {
            String token = googleTokenService.getAccessToken();
            response.put("token_available", token != null);
            if (token != null) {
                response.put("token_preview", token.substring(0, 30) + "...");
            }
        }
        
        return response;
    }
}