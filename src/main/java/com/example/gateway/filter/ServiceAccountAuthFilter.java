package com.example.gateway.filter;

import com.example.gateway.security.GoogleTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
@Component
public class ServiceAccountAuthFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAccountAuthFilter.class);

    @Autowired
    private GoogleTokenService googleTokenService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Only add service account token if no user token exists
        String existingAuth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (existingAuth != null && existingAuth.startsWith("Bearer ")) {
            return chain.filter(exchange); // User already authenticated
        }

        // Generate service account token
        String serviceAccountToken = googleTokenService.getAccessToken();
        
        if (serviceAccountToken != null) {
            // Add service account token to downstream request
            ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> {
                    builder.header("X-Service-Account-Token", serviceAccountToken);
                    // Or use Authorization header if downstream expects it
                    builder.header("Authorization", "Bearer " + serviceAccountToken);
                })
                .build();

            logger.debug("Added service account token to downstream request");
            return chain.filter(mutatedExchange);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 1; // Run after security but before other filters
    }
}