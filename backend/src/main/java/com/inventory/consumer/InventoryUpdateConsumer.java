package com.inventory.consumer;

import com.inventory.event.InventoryUpdateEvent;
import com.inventory.model.Inventory;
import com.inventory.model.Transaction;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StoreRepository;
import com.inventory.repository.TransactionRepository;
import com.inventory.publisher.InventoryEventPublisher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @KafkaListener(topics = {"inventory.commands.stock"}, groupId = "inventory-service")
    @Transactional
    public void handleInventoryUpdate(ConsumerRecord<String, InventoryUpdateEvent> record, Acknowledgment ack) {
        InventoryUpdateEvent event = record.value();
        try {
            System.out.println("🔄 Processing inventory update event (Kafka): " + event.getEventId());

            Inventory inventory = inventoryRepository
                    .findByStoreIdAndProductId(event.getStoreId(), event.getProductId())
                    .orElseGet(() -> {
                        var store = storeRepository.findById(event.getStoreId())
                                .orElseThrow(() -> new RuntimeException("Store not found: " + event.getStoreId()));
                        var product = productRepository.findById(event.getProductId())
                                .orElseThrow(() -> new RuntimeException("Product not found: " + event.getProductId()));
                        return new Inventory(store, product, 0);
                    });

            if (event.getVersion() != null && !event.getVersion().equals(inventory.getVersion())) {
                throw new RuntimeException("Inventory was modified by another transaction. Expected version: "
                        + event.getVersion() + ", actual: " + inventory.getVersion());
            }

            int oldQuantity = inventory.getQuantity();
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

            eventPublisher.publishAuditEvent(
                    "UPDATE",
                    event.getStoreId(),
                    event.getProductId(),
                    oldQuantity,
                    inventory.getQuantity()
            );

            messagingTemplate.convertAndSend("/topic/inventory-updates", inventory);

            if (ack != null) ack.acknowledge();
            System.out.println("✅ Successfully processed inventory update (Kafka): " + event.getEventId() +
                    " - Qty: " + oldQuantity + " -> " + inventory.getQuantity());
        } catch (Exception e) {
            System.err.println("❌ Failed to process inventory update event (Kafka): " + event.getEventId() + " - " + e.getMessage());
            if (ack != null) {
                try { ack.nack(1000); } catch (Exception ignore) {}
            }
        }
    }
}