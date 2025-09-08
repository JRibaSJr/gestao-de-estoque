package com.inventory.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder, RedisRateLimiter redisRateLimiter, KeyResolver userKeyResolver) {
		return builder.routes()
			// Inventory Service Routes
			.route("inventory-service", r -> r.path("/api/inventory/**")
				.filters(f -> f
					.circuitBreaker(config -> config
						.setName("inventory-service")
						.setFallbackUri("forward:/fallback/inventory"))
					.requestRateLimiter(config -> config
						.setRateLimiter(redisRateLimiter)
						.setKeyResolver(userKeyResolver))
					.addRequestHeader("X-Gateway-Source", "api-gateway")
					.addResponseHeader("X-Gateway-Response-Time", String.valueOf(System.currentTimeMillis())))
				.uri("http://localhost:8001"))
				
			// Store Service Routes  
			.route("store-service", r -> r.path("/api/stores/**")
				.filters(f -> f
					.circuitBreaker(config -> config
						.setName("store-service")
						.setFallbackUri("forward:/fallback/stores"))
					.requestRateLimiter(config -> config
						.setRateLimiter(redisRateLimiter)
						.setKeyResolver(userKeyResolver))
					.addRequestHeader("X-Gateway-Source", "api-gateway"))
				.uri("http://localhost:8001"))
				
			// Product Service Routes
			.route("product-service", r -> r.path("/api/products/**")
				.filters(f -> f
					.circuitBreaker(config -> config
						.setName("product-service")
						.setFallbackUri("forward:/fallback/products"))
					.requestRateLimiter(config -> config
						.setRateLimiter(redisRateLimiter())
						.setKeyResolver(userKeyResolver()))
					.addRequestHeader("X-Gateway-Source", "api-gateway"))
				.uri("http://localhost:8001"))
				
			// Transaction Service Routes
			.route("transaction-service", r -> r.path("/api/transactions/**")
				.filters(f -> f
					.circuitBreaker(config -> config
						.setName("transaction-service")
						.setFallbackUri("forward:/fallback/transactions"))
					.requestRateLimiter(config -> config
						.setRateLimiter(redisRateLimiter())
						.setKeyResolver(userKeyResolver()))
					.addRequestHeader("X-Gateway-Source", "api-gateway"))
				.uri("http://localhost:8001"))
				
			// Sync Service Routes
			.route("sync-service", r -> r.path("/api/sync/**")
				.filters(f -> f
					.circuitBreaker(config -> config
						.setName("sync-service")
						.setFallbackUri("forward:/fallback/sync"))
					.requestRateLimiter(config -> config
						.setRateLimiter(redisRateLimiter())
						.setKeyResolver(userKeyResolver()))
					.addRequestHeader("X-Gateway-Source", "api-gateway"))
				.uri("http://localhost:8001"))
				
			// Health Check Routes (No rate limiting)
			.route("health-check", r -> r.path("/actuator/health", "/api/actuator/health")
				.filters(f -> f
					.addRequestHeader("X-Gateway-Source", "api-gateway"))
				.uri("http://localhost:8001"))
				
			// Test Routes
			.route("test-service", r -> r.path("/api/test/**")
				.filters(f -> f
					.circuitBreaker(config -> config
						.setName("test-service")
						.setFallbackUri("forward:/fallback/test"))
					.requestRateLimiter(config -> config
						.setRateLimiter(redisRateLimiter())
						.setKeyResolver(userKeyResolver()))
					.addRequestHeader("X-Gateway-Source", "api-gateway"))
				.uri("http://localhost:8001"))
			.build();
	}
	
	// Removed duplicate beans - using ones from GatewayConfig
}