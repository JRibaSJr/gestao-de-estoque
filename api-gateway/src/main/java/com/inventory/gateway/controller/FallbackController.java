package com.inventory.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/inventory")
    @PostMapping("/inventory")
    public ResponseEntity<Map<String, Object>> inventoryFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            Map.of(
                "error", "Inventory service is currently unavailable",
                "message", "Please try again later. The request has been queued for processing.",
                "timestamp", LocalDateTime.now(),
                "service", "inventory-service",
                "fallback", true
            )
        );
    }

    @GetMapping("/stores")  
    @PostMapping("/stores")
    public ResponseEntity<Map<String, Object>> storesFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            Map.of(
                "error", "Store service is currently unavailable",
                "message", "Please try again later",
                "timestamp", LocalDateTime.now(),
                "service", "store-service",
                "fallback", true
            )
        );
    }

    @GetMapping("/products")
    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> productsFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            Map.of(
                "error", "Product service is currently unavailable", 
                "message", "Please try again later",
                "timestamp", LocalDateTime.now(),
                "service", "product-service",
                "fallback", true
            )
        );
    }

    @GetMapping("/transactions")
    @PostMapping("/transactions")
    public ResponseEntity<Map<String, Object>> transactionsFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            Map.of(
                "error", "Transaction service is currently unavailable",
                "message", "Please try again later",
                "timestamp", LocalDateTime.now(),
                "service", "transaction-service", 
                "fallback", true
            )
        );
    }

    @GetMapping("/sync")
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            Map.of(
                "error", "Sync service is currently unavailable",
                "message", "Synchronization will be retried automatically",
                "timestamp", LocalDateTime.now(),
                "service", "sync-service",
                "fallback", true
            )
        );
    }

    @GetMapping("/test")
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            Map.of(
                "error", "Test service is currently unavailable",
                "message", "Please try again later",
                "timestamp", LocalDateTime.now(),
                "service", "test-service",
                "fallback", true
            )
        );
    }
}