package com.inventory.consumer;

// RabbitMQConfig removed after Kafka migration
import com.inventory.event.InventoryAuditEvent;
import com.inventory.model.SyncEvent;
import com.inventory.repository.SyncEventRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rabbitmq.client.Channel;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class InventoryAuditConsumer {

    @Autowired
    private SyncEventRepository syncEventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.INVENTORY_AUDIT_QUEUE)
    @Transactional
    public void handleInventoryAudit(InventoryAuditEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
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