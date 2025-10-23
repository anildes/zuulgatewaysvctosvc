package com.example.gateway.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Service
public class GoogleTokenService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTokenService.class);

    @Value("${google.scopes:https://www.googleapis.com/auth/cloud-platform}")
    private List<String> scopes;

    @Value("${google.credentials.path:}")
    private String credentialsPath;

    private GoogleCredentials credentials;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        try {
            if (credentialsPath != null && !credentialsPath.isEmpty()) {
                // Load from specific path
                logger.info("Loading Google credentials from: {}", credentialsPath);
                credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                    .createScoped(scopes);
            } else {
                // Load from environment variable
                logger.info("Loading Google credentials from ADC");
                credentials = GoogleCredentials.getApplicationDefault().createScoped(scopes);
            }
            
            // Test the credentials
            credentials.refresh();
            initialized = true;
            logger.info("✅ Google Service Account initialized successfully");

        } catch (IOException e) {
            logger.error("❌ Failed to initialize Google Service Account: {}", e.getMessage());
            logger.info("💡 To fix this:");
            logger.info("1. Create a service account in Google Cloud Console");
            logger.info("2. Download JSON key file");
            logger.info("3. Set GOOGLE_APPLICATION_CREDENTIALS environment variable");
            initialized = false;
        }
    }

    /**
     * Get access token for service-to-service authentication
     */
    public String getAccessToken() {
        if (!initialized) {
            logger.warn("Google Service Account not initialized");
            return null;
        }

        try {
            credentials.refresh();
            String token = credentials.getAccessToken().getTokenValue();
            logger.debug("Generated service account token: {}...", token.substring(0, 50));
            return token;
        } catch (IOException e) {
            logger.error("Error refreshing service account token", e);
            return null;
        }
    }

    /**
     * Get ID token for specific audience (if calling other services)
     */
    public String getIdToken(String audience) {
        if (!initialized) {
            return null;
        }

        try {
            IdTokenCredentials idTokenCredentials = IdTokenCredentials.newBuilder()
                .setIdTokenProvider((IdTokenProvider) credentials)
                .setTargetAudience(audience)
                .build();

            idTokenCredentials.refresh();
            return idTokenCredentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            logger.error("Error generating ID token", e);
            return null;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}