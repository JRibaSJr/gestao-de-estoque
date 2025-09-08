package com.inventory.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayInfoController {

    @Autowired
    private RouteLocator routeLocator;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getGatewayInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("gateway", "Distributed Inventory System API Gateway");
        info.put("version", "1.0.0");
        info.put("timestamp", LocalDateTime.now());
        info.put("port", 8080);
        info.put("features", List.of(
            "Request Routing",
            "Rate Limiting", 
            "Circuit Breaker",
            "Authentication/Authorization",
            "CORS Handling",
            "Request/Response Logging",
            "Fallback Responses"
        ));
        
        return ResponseEntity.ok(info);
    }

    @GetMapping("/routes")
    public ResponseEntity<List<Map<String, Object>>> getRoutes() {
        return ResponseEntity.ok(
            routeLocator.getRoutes()
                .map(this::routeToMap)
                .collectList()
                .block()
        );
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("gateway", "operational");
        health.put("timestamp", LocalDateTime.now());
        health.put("services", Map.of(
            "inventory-service", "http://localhost:8001",
            "store-service", "http://localhost:8001", 
            "product-service", "http://localhost:8001",
            "transaction-service", "http://localhost:8001",
            "sync-service", "http://localhost:8001"
        ));
        
        return ResponseEntity.ok(health);
    }

    private Map<String, Object> routeToMap(Route route) {
        Map<String, Object> routeMap = new HashMap<>();
        routeMap.put("id", route.getId());
        routeMap.put("uri", route.getUri().toString());
        routeMap.put("predicate", route.getPredicate().toString());
        routeMap.put("filters", route.getFilters().size());
        return routeMap;
    }
}