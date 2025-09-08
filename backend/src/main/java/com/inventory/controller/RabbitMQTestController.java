// package com.inventory.controller; // Deprecated after Kafka migration

import com.inventory.publisher.InventoryEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class RabbitMQTestController {

    @Autowired
    private InventoryEventPublisher eventPublisher;

    @PostMapping("/rabbitmq")
    public ResponseEntity<Map<String, String>> testRabbitMQ() {
        try {
            // Test simple message publishing
            eventPublisher.publishUpdateEvent(1L, 1L, 5, "ADD", "RabbitMQ test message");
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "RabbitMQ message published successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error", 
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/rabbitmq/status")
    public ResponseEntity<Map<String, String>> rabbitMQStatus() {
        return ResponseEntity.ok(Map.of(
            "rabbitmq", "configured",
            "queues", "inventory.update.queue, inventory.transfer.queue, inventory.sync.queue, inventory.audit.queue",
            "status", "ready"
        ));
    }
}