package com.inventory.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/inventory/subscribe")
    @SendTo("/topic/inventory-updates")
    public Map<String, Object> subscribeToInventoryUpdates() {
        return Map.of(
            "message", "Subscribed to inventory updates",
            "timestamp", LocalDateTime.now()
        );
    }

    @MessageMapping("/sync/subscribe")
    @SendTo("/topic/sync-updates")
    public Map<String, Object> subscribeToSyncUpdates() {
        return Map.of(
            "message", "Subscribed to sync updates",
            "timestamp", LocalDateTime.now()
        );
    }

    public void sendInventoryUpdate(Object update) {
        messagingTemplate.convertAndSend("/topic/inventory-updates", update);
    }

    public void sendSyncUpdate(Object update) {
        messagingTemplate.convertAndSend("/topic/sync-updates", update);
    }

    public void sendSystemAlert(Object alert) {
        messagingTemplate.convertAndSend("/topic/system-alerts", alert);
    }
}