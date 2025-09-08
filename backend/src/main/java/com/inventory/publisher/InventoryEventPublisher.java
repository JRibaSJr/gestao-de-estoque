package com.inventory.publisher;

import com.inventory.event.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventPublisher {

    @Autowired
    private KafkaTemplate&lt;String, Object&gt; kafkaTemplate;

    public void publishInventoryUpdate(InventoryUpdateEvent event) {
        try {
            kafkaTemplate.send("inventory.commands.stock", event.getCorrelationId(), event);
            System.out.println("✅ Published inventory update event (Kafka): " + event.getEventId());
        } catch (Exception e) {
            System.err.println("❌ Failed to publish inventory update event (Kafka): " + e.getMessage());
            throw new RuntimeException("Failed to publish inventory update event", e);
        }
    }

    public void publishInventoryTransfer(InventoryTransferEvent event) {
        try {
            kafkaTemplate.send("transfers.commands", event.getSagaId() != null ? event.getSagaId() : event.getCorrelationId(), event);
            System.out.println("✅ Published inventory transfer event (Kafka): " + event.getEventId());
        } catch (Exception e) {
            System.err.println("❌ Failed to publish inventory transfer event (Kafka): " + e.getMessage());
            throw new RuntimeException("Failed to publish inventory transfer event", e);
        }
    }

    public void publishInventorySync(InventorySyncEvent event) {
        try {
            kafkaTemplate.send("inventory.events", event.getCorrelationId(), event);
            System.out.println("✅ Published inventory sync event (Kafka): " + event.getEventId());
        } catch (Exception e) {
            System.err.println("❌ Failed to publish inventory sync event (Kafka): " + e.getMessage());
            throw new RuntimeException("Failed to publish inventory sync event", e);
        }
    }

    public void publishInventoryAudit(InventoryAuditEvent event) {
        try {
            kafkaTemplate.send("notifications.events", event.getCorrelationId(), event);
            System.out.println("✅ Published inventory audit event (Kafka): " + event.getEventId());
        } catch (Exception e) {
            System.err.println("❌ Failed to publish inventory audit event (Kafka): " + e.getMessage());
        }
    }

    // Convenience methods
    public void publishUpdateEvent(Long storeId, Long productId, Integer quantityChange, String operation, String notes) {
        InventoryUpdateEvent event = new InventoryUpdateEvent(storeId, productId, quantityChange, operation);
        event.setNotes(notes);
        event.setCorrelationId(java.util.UUID.randomUUID().toString());
        publishInventoryUpdate(event);
    }
}