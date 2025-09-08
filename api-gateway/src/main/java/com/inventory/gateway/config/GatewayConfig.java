package com.inventory.gateway.config;

import com.inventory.gateway.filter.AuthenticationFilter;
import com.inventory.gateway.filter.RequestLoggingFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // 100 requests per second with burst capacity of 200
        return new RedisRateLimiter(100, 200, 1);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get user ID from JWT token
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }
            
            // Fallback to IP address
            String clientIP = exchange.getRequest().getRemoteAddress() != null ? 
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
            return Mono.just("ip:" + clientIP);
        };
    }

    @Bean
    public AuthenticationFilter authenticationFilter() {
        return new AuthenticationFilter();
    }

    @Bean  
    public RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }
}