package com.inventory.saga;

import com.inventory.event.InventoryTransferEvent;
import com.inventory.event.InventoryUpdateEvent;
import com.inventory.model.Inventory;
import com.inventory.model.Transaction;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.StoreRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.TransactionRepository;
import com.inventory.publisher.InventoryEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryTransferSaga {

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

    @Transactional
    public void startTransfer(InventoryTransferEvent event) {
        try {
            System.out.println("🚀 Starting transfer saga: " + event.getSagaId());
            
            // Validate transfer can proceed
            Optional<Inventory> sourceInventory = inventoryRepository
                    .findByStoreIdAndProductIdForUpdate(event.getFromStoreId(), event.getProductId());
            
            if (sourceInventory.isEmpty()) {
                throw new RuntimeException("Source inventory not found");
            }
            
            if (!sourceInventory.get().canReserve(event.getQuantity())) {
                throw new RuntimeException("Insufficient quantity available for transfer");
            }

            // Step 1: Reserve inventory at source
            InventoryTransferEvent reserveEvent = new InventoryTransferEvent(
                event.getFromStoreId(), 
                event.getToStoreId(), 
                event.getProductId(), 
                event.getQuantity(), 
                "RESERVE"
            );
            reserveEvent.setSagaId(event.getSagaId());
            reserveEvent.setNotes("Transfer reservation: " + event.getNotes());
            
            eventPublisher.publishInventoryTransfer(reserveEvent);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to start transfer saga: " + e.getMessage());
            rollbackTransfer(event);
            throw e;
        }
    }

    @Transactional
    public void reserveInventory(InventoryTransferEvent event) {
        try {
            System.out.println("🔒 Reserving inventory for saga: " + event.getSagaId());
            
            // Reserve inventory at source store
            Inventory sourceInventory = inventoryRepository
                    .findByStoreIdAndProductIdForUpdate(event.getFromStoreId(), event.getProductId())
                    .orElseThrow(() -> new RuntimeException("Source inventory not found"));
            
            sourceInventory.reserve(event.getQuantity());
            inventoryRepository.save(sourceInventory);
            
            // Create reservation transaction
            Transaction reserveTransaction = new Transaction(
                sourceInventory.getStore(),
                sourceInventory.getProduct(),
                Transaction.TransactionType.TRANSFER_OUT,
                event.getQuantity(),
                event.getSagaId(),
                "Reserved for transfer: " + event.getNotes()
            );
            transactionRepository.save(reserveTransaction);

            // Step 2: Add inventory at destination
            InventoryTransferEvent confirmEvent = new InventoryTransferEvent(
                event.getFromStoreId(), 
                event.getToStoreId(), 
                event.getProductId(), 
                event.getQuantity(), 
                "CONFIRM"
            );
            confirmEvent.setSagaId(event.getSagaId());
            confirmEvent.setNotes(event.getNotes());
            
            eventPublisher.publishInventoryTransfer(confirmEvent);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to reserve inventory: " + e.getMessage());
            rollbackTransfer(event);
            throw e;
        }
    }

    @Transactional
    public void confirmTransfer(InventoryTransferEvent event) {
        try {
            System.out.println("✅ Confirming transfer for saga: " + event.getSagaId());
            
            // Add inventory at destination store
            Inventory destInventory = inventoryRepository
                    .findByStoreIdAndProductIdForUpdate(event.getToStoreId(), event.getProductId())
                    .orElseGet(() -> {
                        var store = storeRepository.findById(event.getToStoreId())
                                .orElseThrow(() -> new RuntimeException("Destination store not found"));
                        var product = productRepository.findById(event.getProductId())
                                .orElseThrow(() -> new RuntimeException("Product not found"));
                        return new Inventory(store, product, 0);
                    });
            
            destInventory.adjustQuantity(event.getQuantity());
            inventoryRepository.save(destInventory);
            
            // Create destination transaction
            Transaction destTransaction = new Transaction(
                destInventory.getStore(),
                destInventory.getProduct(),
                Transaction.TransactionType.TRANSFER_IN,
                event.getQuantity(),
                event.getSagaId(),
                "Received from transfer: " + event.getNotes()
            );
            transactionRepository.save(destTransaction);

            // Final step: Release reservation and complete transfer
            Inventory sourceInventory = inventoryRepository
                    .findByStoreIdAndProductIdForUpdate(event.getFromStoreId(), event.getProductId())
                    .orElseThrow(() -> new RuntimeException("Source inventory not found"));
            
            // Release reservation and actually reduce quantity
            sourceInventory.releaseReservation(event.getQuantity());
            sourceInventory.adjustQuantity(-event.getQuantity());
            inventoryRepository.save(sourceInventory);

            // Publish audit events
            eventPublisher.publishAuditEvent(
                "TRANSFER_COMPLETE", 
                event.getFromStoreId(), 
                event.getProductId(),
                "Transfer to store " + event.getToStoreId(),
                event.getSagaId()
            );

            // Send real-time updates
            messagingTemplate.convertAndSend("/topic/inventory-updates", sourceInventory);
            messagingTemplate.convertAndSend("/topic/inventory-updates", destInventory);
            messagingTemplate.convertAndSend("/topic/transfer-completed", event);
            
            System.out.println("🎉 Transfer saga completed successfully: " + event.getSagaId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to confirm transfer: " + e.getMessage());
            rollbackTransfer(event);
            throw e;
        }
    }

    @Transactional
    public void rollbackTransfer(InventoryTransferEvent event) {
        try {
            System.out.println("🔄 Rolling back transfer saga: " + event.getSagaId());
            
            // Release any reservations
            Optional<Inventory> sourceInventory = inventoryRepository
                    .findByStoreIdAndProductIdForUpdate(event.getFromStoreId(), event.getProductId());
            
            if (sourceInventory.isPresent() && sourceInventory.get().getReservedQuantity() >= event.getQuantity()) {
                sourceInventory.get().releaseReservation(event.getQuantity());
                inventoryRepository.save(sourceInventory.get());
            }

            // Create rollback transaction
            if (sourceInventory.isPresent()) {
                Transaction rollbackTransaction = new Transaction(
                    sourceInventory.get().getStore(),
                    sourceInventory.get().getProduct(),
                    Transaction.TransactionType.ADJUSTMENT,
                    event.getQuantity(),
                    event.getSagaId(),
                    "Transfer rollback: " + event.getNotes()
                );
                transactionRepository.save(rollbackTransaction);
            }

            // Send rollback notification
            messagingTemplate.convertAndSend("/topic/transfer-failed", event);
            
            System.out.println("🔄 Transfer saga rolled back: " + event.getSagaId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to rollback transfer: " + e.getMessage());
        }
    }
}