package com.inventory.consumer;

import com.inventory.config.RabbitMQConfig;
import com.inventory.event.InventoryUpdateEvent;
import com.inventory.model.Inventory;
import com.inventory.model.Transaction;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.StoreRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.TransactionRepository;
import com.inventory.publisher.InventoryEventPublisher;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rabbitmq.client.Channel;

@Service
public class InventoryUpdateConsumer {

    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private InventoryEventPublisher eventPublisher;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.INVENTORY_UPDATE_QUEUE)
    @Transactional
    public void handleInventoryUpdate(InventoryUpdateEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            System.out.println("🔄 Processing inventory update event: " + event.getEventId());
            
            // Find or create inventory record with pessimistic locking
            Inventory inventory = inventoryRepository
                    .findByStoreIdAndProductIdForUpdate(event.getStoreId(), event.getProductId())
                    .orElseGet(() -> {
                        var store = storeRepository.findById(event.getStoreId())
                                .orElseThrow(() -> new RuntimeException("Store not found: " + event.getStoreId()));
                        var product = productRepository.findById(event.getProductId())
                                .orElseThrow(() -> new RuntimeException("Product not found: " + event.getProductId()));
                        return new Inventory(store, product, 0);
                    });

            // Optimistic locking check
            if (event.getVersion() != null && !event.getVersion().equals(inventory.getVersion())) {
                throw new RuntimeException("Inventory was modified by another transaction. Expected version: " 
                    + event.getVersion() + ", actual: " + inventory.getVersion());
            }

            int oldQuantity = inventory.getQuantity();
            
            // Apply operation and determine transaction type
            Transaction.TransactionType transactionType;
            
            switch (event.getOperation().toUpperCase()) {
                case "ADD":
                    inventory.adjustQuantity(event.getQuantityChange());
                    transactionType = Transaction.TransactionType.ADJUSTMENT;
                    break;
                case "SUBTRACT":
                    inventory.adjustQuantity(-Math.abs(event.getQuantityChange()));
                    transactionType = Transaction.TransactionType.ADJUSTMENT;
                    break;
                case "SET":
                    inventory.setQuantity(event.getNewQuantity());
                    transactionType = Transaction.TransactionType.ADJUSTMENT;
                    break;
                case "STOCK_IN":
                    inventory.adjustQuantity(Math.abs(event.getQuantityChange()));
                    transactionType = Transaction.TransactionType.STOCK_IN;
                    break;
                case "STOCK_OUT":
                    inventory.adjustQuantity(-Math.abs(event.getQuantityChange()));
                    transactionType = Transaction.TransactionType.STOCK_OUT;
                    break;
                case "RESERVE":
                    inventory.adjustQuantity(-Math.abs(event.getQuantityChange()));
                    transactionType = Transaction.TransactionType.RESERVATION;
                    break;
                case "RELEASE":
                    inventory.adjustQuantity(Math.abs(event.getQuantityChange()));
                    transactionType = Transaction.TransactionType.RELEASE;
                    break;
                default:
                    throw new RuntimeException("Unknown operation: " + event.getOperation());
            }

            // Save inventory
            inventory = inventoryRepository.save(inventory);
                
            Transaction transaction = new Transaction(
                inventory.getStore(),
                inventory.getProduct(),
                transactionType,
                Math.abs(event.getQuantityChange()),
                event.getReferenceId(),
                event.getNotes()
            );
            transactionRepository.save(transaction);

            // Publish audit event
            eventPublisher.publishAuditEvent(
                "UPDATE", 
                event.getStoreId(), 
                event.getProductId(),
                oldQuantity,
                inventory.getQuantity()
            );

            // Send real-time update via WebSocket
            messagingTemplate.convertAndSend("/topic/inventory-updates", inventory);

            // ACK the message
            channel.basicAck(deliveryTag, false);
            
            System.out.println("✅ Successfully processed inventory update: " + event.getEventId() + 
                " - Quantity changed from " + oldQuantity + " to " + inventory.getQuantity());

        } catch (Exception e) {
            System.err.println("❌ Failed to process inventory update event: " + event.getEventId() + " - " + e.getMessage());
            try {
                // NACK and requeue (will go to DLQ after max retries)
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackError) {
                System.err.println("❌ Failed to NACK message: " + nackError.getMessage());
            }
        }
    }
}