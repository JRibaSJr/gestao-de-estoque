package com.inventory.service;

import com.inventory.dto.InventoryDTO;
import com.inventory.dto.InventoryUpdateRequest;
import com.inventory.model.Inventory;
import com.inventory.model.Store;
import com.inventory.model.Product;
import com.inventory.model.Transaction;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.StoreRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<InventoryDTO> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(InventoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InventoryDTO> getInventoryByStore(Long storeId) {
        return inventoryRepository.findByStoreIdWithDetails(storeId).stream()
                .map(InventoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InventoryDTO> getInventoryByProduct(Long productId) {
        return inventoryRepository.findByProductId(productId).stream()
                .map(InventoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<InventoryDTO> getInventoryByStoreAndProduct(Long storeId, Long productId) {
        return inventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .map(InventoryDTO::fromEntity);
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "updateInventoryFallback")
    @Retry(name = "inventory-service")
    public InventoryDTO updateInventory(InventoryUpdateRequest request) {
        // Use pessimistic locking for consistency
        Optional<Inventory> inventoryOpt = inventoryRepository
                .findByStoreIdAndProductIdForUpdate(request.getStoreId(), request.getProductId());
        
        Inventory inventory;
        boolean isNewRecord = false;
        
        if (inventoryOpt.isPresent()) {
            inventory = inventoryOpt.get();
            
            // Optimistic locking check
            if (request.getVersion() != null && !request.getVersion().equals(inventory.getVersion())) {
                throw new RuntimeException("Inventory record was modified by another transaction");
            }
        } else {
            // Create new inventory record
            Store store = storeRepository.findById(request.getStoreId())
                    .orElseThrow(() -> new RuntimeException("Store not found"));
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            
            inventory = new Inventory(store, product, 0);
            isNewRecord = true;
        }
        
        // Apply quantity adjustment
        inventory.adjustQuantity(request.getQuantityAdjustment());
        inventory = inventoryRepository.save(inventory);
        
        // Create transaction record
        Transaction.TransactionType transactionType = request.getQuantityAdjustment() > 0 
                ? Transaction.TransactionType.STOCK_IN 
                : Transaction.TransactionType.STOCK_OUT;
                
        Transaction transaction = new Transaction(
                inventory.getStore(),
                inventory.getProduct(),
                transactionType,
                Math.abs(request.getQuantityAdjustment()),
                request.getReferenceId(),
                request.getNotes()
        );
        transactionRepository.save(transaction);
        
        // Send real-time update
        InventoryDTO result = InventoryDTO.fromEntity(inventory);
        messagingTemplate.convertAndSend("/topic/inventory-updates", result);
        
        return result;
    }

    public InventoryDTO updateInventoryFallback(InventoryUpdateRequest request, Exception ex) {
        // Fallback method for circuit breaker
        throw new RuntimeException("Inventory service is currently unavailable. Please try again later.", ex);
    }

    @CircuitBreaker(name = "inventory-service")
    public InventoryDTO transferInventory(Long fromStoreId, Long toStoreId, Long productId, Integer quantity, String notes) {
        String referenceId = UUID.randomUUID().toString();
        
        // Reduce inventory from source store
        InventoryUpdateRequest reduceRequest = new InventoryUpdateRequest(fromStoreId, productId, -quantity);
        reduceRequest.setReferenceId(referenceId);
        reduceRequest.setNotes("Transfer OUT: " + notes);
        updateInventory(reduceRequest);
        
        // Increase inventory in destination store  
        InventoryUpdateRequest increaseRequest = new InventoryUpdateRequest(toStoreId, productId, quantity);
        increaseRequest.setReferenceId(referenceId);
        increaseRequest.setNotes("Transfer IN: " + notes);
        return updateInventory(increaseRequest);
    }

    public List<InventoryDTO> getLowStockItems(Integer threshold) {
        return inventoryRepository.findLowStockItems(threshold != null ? threshold : 10).stream()
                .map(InventoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public InventoryDTO reserveInventory(Long storeId, Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByStoreIdAndProductIdForUpdate(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        
        inventory.reserve(quantity);
        inventory = inventoryRepository.save(inventory);
        
        // Create reservation transaction
        Transaction transaction = new Transaction(
                inventory.getStore(),
                inventory.getProduct(),
                Transaction.TransactionType.RESERVATION,
                quantity,
                UUID.randomUUID().toString(),
                "Inventory reserved"
        );
        transactionRepository.save(transaction);
        
        InventoryDTO result = InventoryDTO.fromEntity(inventory);
        messagingTemplate.convertAndSend("/topic/inventory-updates", result);
        
        return result;
    }

    public InventoryDTO releaseReservation(Long storeId, Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByStoreIdAndProductIdForUpdate(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        
        inventory.releaseReservation(quantity);
        inventory = inventoryRepository.save(inventory);
        
        // Create release transaction
        Transaction transaction = new Transaction(
                inventory.getStore(),
                inventory.getProduct(),
                Transaction.TransactionType.RELEASE,
                quantity,
                UUID.randomUUID().toString(),
                "Reservation released"
        );
        transactionRepository.save(transaction);
        
        InventoryDTO result = InventoryDTO.fromEntity(inventory);
        messagingTemplate.convertAndSend("/topic/inventory-updates", result);
        
        return result;
    }
}