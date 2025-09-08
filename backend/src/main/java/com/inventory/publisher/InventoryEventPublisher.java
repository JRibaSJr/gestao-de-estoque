package com.inventory.publisher;

import com.inventory.config.RabbitMQConfig;
import com.inventory.event.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InventoryEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void publishInventoryUpdate(InventoryUpdateEvent event) {
        try {
            MessageProperties properties = new MessageProperties();
            properties.setPriority(event.getPriority());
            properties.setTimestamp(java.sql.Timestamp.valueOf(event.getTimestamp()));
            properties.setCorrelationId(event.getCorrelationId());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INVENTORY_EXCHANGE,
                RabbitMQConfig.INVENTORY_UPDATE_KEY,
                event,
                message -> {
                    message.getMessageProperties().setPriority(event.getPriority());
                    return message;
                }
            );
            
            System.out.println("✅ Published inventory update event: " + event.getEventId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to publish inventory update event: " + e.getMessage());
            throw new RuntimeException("Failed to publish inventory update event", e);
        }
    }

    public void publishInventoryTransfer(InventoryTransferEvent event) {
        try {
            MessageProperties properties = new MessageProperties();
            properties.setPriority(event.getPriority());
            properties.setTimestamp(java.sql.Timestamp.valueOf(event.getTimestamp()));
            properties.setCorrelationId(event.getCorrelationId());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INVENTORY_EXCHANGE,
                RabbitMQConfig.INVENTORY_TRANSFER_KEY,
                event,
                message -> {
                    message.getMessageProperties().setPriority(event.getPriority());
                    return message;
                }
            );
            
            System.out.println("✅ Published inventory transfer event: " + event.getEventId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to publish inventory transfer event: " + e.getMessage());
            throw new RuntimeException("Failed to publish inventory transfer event", e);
        }
    }

    public void publishInventorySync(InventorySyncEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INVENTORY_EXCHANGE,
                RabbitMQConfig.INVENTORY_SYNC_KEY,
                event,
                message -> {
                    message.getMessageProperties().setPriority(event.getPriority());
                    return message;
                }
            );
            
            System.out.println("✅ Published inventory sync event: " + event.getEventId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to publish inventory sync event: " + e.getMessage());
            throw new RuntimeException("Failed to publish inventory sync event", e);
        }
    }

    public void publishInventoryAudit(InventoryAuditEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INVENTORY_EXCHANGE,
                RabbitMQConfig.INVENTORY_AUDIT_KEY,
                event
            );
            
            System.out.println("✅ Published inventory audit event: " + event.getEventId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to publish inventory audit event: " + e.getMessage());
            // Don't throw exception for audit events to avoid blocking main operations
        }
    }

    // Convenience methods
    public void publishUpdateEvent(Long storeId, Long productId, Integer quantityChange, String operation, String notes) {
        InventoryUpdateEvent event = new InventoryUpdateEvent(storeId, productId, quantityChange, operation);
        event.setNotes(notes);
        event.setCorrelationId(java.util.UUID.randomUUID().toString());
        publishInventoryUpdate(event);
    }

    public void publishTransferEvent(Long fromStoreId, Long toStoreId, Long productId, Integer quantity, String transferType, String sagaId) {
        InventoryTransferEvent event = new InventoryTransferEvent(fromStoreId, toStoreId, productId, quantity, transferType);
        event.setSagaId(sagaId);
        event.setCorrelationId(sagaId); // Use sagaId as correlation
        publishInventoryTransfer(event);
    }

    public void publishSyncEvent(String syncType, Long storeId) {
        InventorySyncEvent event = new InventorySyncEvent(syncType, storeId);
        event.setCorrelationId(java.util.UUID.randomUUID().toString());
        publishInventorySync(event);
    }

    public void publishAuditEvent(String action, Long storeId, Long productId, Object oldValue, Object newValue) {
        InventoryAuditEvent event = new InventoryAuditEvent(action, storeId, productId, oldValue, newValue);
        event.setCorrelationId(java.util.UUID.randomUUID().toString());
        publishInventoryAudit(event);
    }
}