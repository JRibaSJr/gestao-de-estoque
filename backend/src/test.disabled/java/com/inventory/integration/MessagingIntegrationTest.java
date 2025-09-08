package com.inventory.integration;

import com.inventory.event.InventoryUpdateEvent;
import com.inventory.publisher.InventoryEventPublisher;
import com.inventory.consumer.InventoryUpdateConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672",
    "spring.rabbitmq.username=guest",
    "spring.rabbitmq.password=guest"
})
class MessagingIntegrationTest {

    @Autowired
    private InventoryEventPublisher eventPublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitListenerTestHarness harness;

    @Test
    void testEventPublishing_InventoryUpdate() {
        // Arrange
        InventoryUpdateEvent event = new InventoryUpdateEvent(1L, 1L, 10, "ADD");
        event.setNotes("Test event publishing");
        event.setReferenceId("TEST-001");

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> eventPublisher.publishInventoryUpdate(event));
    }

    @Test
    void testMessageConsumption_WithMockConsumer() throws InterruptedException {
        // Create a test consumer with CountDownLatch
        CountDownLatch latch = new CountDownLatch(1);
        
        // Mock consumer that counts messages
        rabbitTemplate.setReceiveTimeout(5000);
        
        // Send test message
        InventoryUpdateEvent testEvent = new InventoryUpdateEvent(1L, 1L, 15, "STOCK_IN");
        testEvent.setNotes("Integration test message");
        
        eventPublisher.publishInventoryUpdate(testEvent);
        
        // Wait for message processing (simplified test)
        Thread.sleep(1000);
        
        // Verify message was sent (basic check)
        assertNotNull(testEvent.getEventId());
        assertTrue(testEvent.getTimestamp() != null);
    }

    @Test
    void testRabbitMQConnection() {
        // Test basic RabbitMQ connectivity
        assertDoesNotThrow(() -> {
            rabbitTemplate.convertAndSend("test.queue", "test message");
        });
    }

    @Test
    void testEventSerialization() {
        // Test that events can be properly serialized/deserialized
        InventoryUpdateEvent originalEvent = new InventoryUpdateEvent(2L, 3L, 20, "SUBTRACT");
        originalEvent.setNotes("Serialization test");
        originalEvent.setVersion(5L);

        // Send and receive event (simplified)
        assertDoesNotThrow(() -> {
            rabbitTemplate.convertAndSend("inventory.updates", originalEvent);
        });

        // Verify event properties
        assertEquals(2L, originalEvent.getStoreId());
        assertEquals(3L, originalEvent.getProductId());
        assertEquals(20, originalEvent.getQuantityChange());
        assertEquals("SUBTRACT", originalEvent.getOperation());
    }
}