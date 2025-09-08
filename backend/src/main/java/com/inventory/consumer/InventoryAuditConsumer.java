package com.inventory.consumer;

// RabbitMQConfig removed after Kafka migration
import com.inventory.event.InventoryAuditEvent;
import com.inventory.model.SyncEvent;
import com.inventory.repository.SyncEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class InventoryAuditConsumer {

    @Autowired
    private SyncEventRepository syncEventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = {"notifications.events"}, groupId = "inventory-service")
    @Transactional
    public void handleInventoryAudit(ConsumerRecord<String, InventoryAuditEvent> record, Acknowledgment ack) {
        InventoryAuditEvent event = record.value();
        try {
            System.out.println("📝 Processing inventory audit event: " + event.getEventId() + " - Action: " + event.getAction());
            
            // Create audit record in sync_events table for persistence
            SyncEvent auditRecord = new SyncEvent();
            auditRecord.setEventType(SyncEvent.EventType.INVENTORY_UPDATE);
            auditRecord.setStatus(SyncEvent.EventStatus.COMPLETED);
            
            // Convert audit data to JSON payload
            String payload = objectMapper.writeValueAsString(event);
            auditRecord.setPayload(payload);
            auditRecord.setProcessedAt(event.getTimestamp());
            
            // Save audit record
            syncEventRepository.save(auditRecord);

            // ACK the message
            channel.basicAck(deliveryTag, false);
            
            System.out.println("✅ Successfully processed audit event: " + event.getEventId());

        } catch (Exception e) {
            System.err.println("❌ Failed to process audit event: " + event.getEventId() + " - " + e.getMessage());
            try {
                // ACK anyway for audit events to avoid blocking
                channel.basicAck(deliveryTag, false);
            } catch (Exception ackError) {
                System.err.println("❌ Failed to ACK audit message: " + ackError.getMessage());
            }
        }
    }
}