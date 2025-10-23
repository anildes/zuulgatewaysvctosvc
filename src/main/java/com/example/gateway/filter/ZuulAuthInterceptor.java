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

/**
 * Gateway Global Filter responsible for intercepting all requests and injecting
 * a valid, non-expired Google-issued Bearer token for service-to-service 
 * communication. This token authenticates the Gateway itself to downstream
 * Resource Servers that trust Google.
 */
@Component
public class ZuulAuthInterceptor implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ZuulAuthInterceptor.class);

    @Autowired(required = false)
    private GoogleTokenService googleTokenService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            // Get the current, non-expired token from the service
            String internalToken = googleTokenService.getAccessToken();

            if (internalToken != null) {
                logger.info("Internal token: " + internalToken);
                
                // Add the Authorization header to the request GOING TO the Resource Server
                // This header uses the token obtained by the Service Account (Client Credentials Flow)
                ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(builder -> builder.header("Authorization", "Bearer " + internalToken))
                    .build();

                logger.debug("Successfully injected Google Service Access Token into downstream request.");
                return chain.filter(mutatedExchange);
            } else {
                logger.error("Failed to retrieve Google Access Token. Downstream request will likely fail authentication.");
            }
            
        } catch (Exception e) {
            logger.error("Error during token injection in Gateway filter.", e);
        }
        
        // Continue with the original exchange if token retrieval failed
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run as one of the very first filters
        return 1;
    }
}