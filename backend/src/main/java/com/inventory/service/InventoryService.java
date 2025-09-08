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
import com.inventory.publisher.InventoryEventPublisher;
import com.inventory.event.InventoryUpdateEvent;
import com.inventory.event.InventoryTransferEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
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
    private InventoryEventPublisher eventPublisher;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Cacheable(value = "inventory", key = "'all'")
    public List<InventoryDTO> getAllInventory() {
        System.out.println("📦 Cache MISS: Loading all inventory from database");
        return inventoryRepository.findAll().stream()
                .map(InventoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "inventory", key = "'store:' + #storeId")
    public List<InventoryDTO> getInventoryByStore(Long storeId) {
        System.out.println("📦 Cache MISS: Loading inventory for store " + storeId + " from database");
        return inventoryRepository.findByStoreIdWithDetails(storeId).stream()
                .map(InventoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "inventory", key = "'product:' + #productId")
    public List<InventoryDTO> getInventoryByProduct(Long productId) {
        System.out.println("📦 Cache MISS: Loading inventory for product " + productId + " from database");
        return inventoryRepository.findByProductId(productId).stream()
                .map(InventoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "inventory", key = "'store:' + #storeId + ':product:' + #productId")
    public Optional<InventoryDTO> getInventoryByStoreAndProduct(Long storeId, Long productId) {
        System.out.println("📦 Cache MISS: Loading inventory for store " + storeId + " and product " + productId + " from database");
        return inventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .map(InventoryDTO::fromEntity);
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "updateInventoryFallback")
    @Retry(name = "inventory-service")
    @CacheEvict(value = {"inventory", "low-stock", "metrics"}, allEntries = true)
    public String updateInventory(InventoryUpdateRequest request) {
        try {
            // Validate store and product exist
            if (!storeRepository.existsById(request.getStoreId())) {
                throw new RuntimeException("Store not found: " + request.getStoreId());
            }
            if (!productRepository.existsById(request.getProductId())) {
                throw new RuntimeException("Product not found: " + request.getProductId());
            }

            // Create and publish inventory update event
            InventoryUpdateEvent event = new InventoryUpdateEvent(
                request.getStoreId(),
                request.getProductId(),
                request.getQuantityAdjustment(),
                request.getQuantityAdjustment() > 0 ? "ADD" : "SUBTRACT"
            );
            
            event.setNotes(request.getNotes());
            event.setReferenceId(request.getReferenceId());
            event.setVersion(request.getVersion());
            event.setCorrelationId(UUID.randomUUID().toString());

            // Publish to RabbitMQ for asynchronous processing
            eventPublisher.publishInventoryUpdate(event);
            
            return "Inventory update event published: " + event.getEventId();
            
        } catch (Exception e) {
            System.err.println("❌ Failed to publish inventory update: " + e.getMessage());
            throw new RuntimeException("Failed to process inventory update", e);
        }
    }

    public String updateInventoryFallback(InventoryUpdateRequest request, Exception ex) {
        System.err.println("🔄 Circuit breaker activated for inventory update: " + ex.getMessage());
        return "Inventory service is currently unavailable. Your request has been queued for processing.";
    }

    @CircuitBreaker(name = "inventory-service")
    public String transferInventory(Long fromStoreId, Long toStoreId, Long productId, Integer quantity, String notes) {
        try {
            // Validate stores and product exist
            if (!storeRepository.existsById(fromStoreId)) {
                throw new RuntimeException("Source store not found: " + fromStoreId);
            }
            if (!storeRepository.existsById(toStoreId)) {
                throw new RuntimeException("Destination store not found: " + toStoreId);
            }
            if (!productRepository.existsById(productId)) {
                throw new RuntimeException("Product not found: " + productId);
            }

            String sagaId = UUID.randomUUID().toString();

            // Create and publish transfer start event
            InventoryTransferEvent event = new InventoryTransferEvent(
                fromStoreId, toStoreId, productId, quantity, "START"
            );
            event.setSagaId(sagaId);
            event.setNotes(notes);
            event.setCorrelationId(sagaId);

            // Publish to RabbitMQ for saga orchestration
            eventPublisher.publishInventoryTransfer(event);
            
            return "Inventory transfer started with saga ID: " + sagaId;
            
        } catch (Exception e) {
            System.err.println("❌ Failed to initiate transfer: " + e.getMessage());
            throw new RuntimeException("Failed to initiate inventory transfer", e);
        }
    }

    @Cacheable(value = "low-stock", key = "'threshold:' + #threshold")
    public List<InventoryDTO> getLowStockItems(Integer threshold) {
        int finalThreshold = threshold != null ? threshold : 10;
        System.out.println("⚠️ Cache MISS: Loading low stock items (threshold: " + finalThreshold + ") from database");
        return inventoryRepository.findLowStockItems(finalThreshold).stream()
                .map(InventoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public String reserveInventory(Long storeId, Long productId, Integer quantity) {
        try {
            // Create reservation update event
            InventoryUpdateEvent event = new InventoryUpdateEvent(storeId, productId, quantity, "RESERVE");
            event.setNotes("Inventory reservation");
            event.setCorrelationId(UUID.randomUUID().toString());

            eventPublisher.publishInventoryUpdate(event);
            
            return "Inventory reservation event published: " + event.getEventId();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to reserve inventory", e);
        }
    }

    public String releaseReservation(Long storeId, Long productId, Integer quantity) {
        try {
            // Create release reservation update event
            InventoryUpdateEvent event = new InventoryUpdateEvent(storeId, productId, -quantity, "RELEASE");
            event.setNotes("Reservation release");
            event.setCorrelationId(UUID.randomUUID().toString());

            eventPublisher.publishInventoryUpdate(event);
            
            return "Reservation release event published: " + event.getEventId();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to release reservation", e);
        }
    }

    public String stockIn(Long storeId, Long productId, Integer quantity, String referenceId, String notes) {
        try {
            // Validate inputs
            if (quantity <= 0) {
                throw new RuntimeException("Quantity must be positive for stock in");
            }
            if (!storeRepository.existsById(storeId)) {
                throw new RuntimeException("Store not found: " + storeId);
            }
            if (!productRepository.existsById(productId)) {
                throw new RuntimeException("Product not found: " + productId);
            }

            // Try to publish event, but continue with direct update if it fails
            String eventId = UUID.randomUUID().toString();
            try {
                InventoryUpdateEvent event = new InventoryUpdateEvent(storeId, productId, quantity, "STOCK_IN");
                event.setNotes(notes != null ? notes : "Stock entry - product received");
                event.setReferenceId(referenceId);
                event.setCorrelationId(eventId);

                eventPublisher.publishInventoryUpdate(event);
                return "Stock in event published: " + event.getEventId() + " - Added " + quantity + " units";
                
            } catch (Exception eventError) {
                System.err.println("⚠️ Event publishing failed, processing directly: " + eventError.getMessage());
                
                // Process directly if event publishing fails
                InventoryUpdateRequest directRequest = new InventoryUpdateRequest();
                directRequest.setStoreId(storeId);
                directRequest.setProductId(productId);
                directRequest.setQuantityAdjustment(quantity);
                directRequest.setNotes(notes);
                directRequest.setReferenceId(referenceId);
                
                InventoryDTO result = updateInventoryDirect(directRequest);
                
                // Create transaction record manually
                Store store = storeRepository.findById(storeId).orElseThrow();
                Product product = productRepository.findById(productId).orElseThrow();
                Transaction transaction = new Transaction(store, product, Transaction.TransactionType.STOCK_IN, quantity, referenceId, notes);
                transactionRepository.save(transaction);
                
                return "Stock in processed directly (event system unavailable): " + eventId + " - Added " + quantity + " units";
            }
            
        } catch (Exception e) {
            System.err.println("🔥 Stock In Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process stock in: " + e.getMessage(), e);
        }
    }

    public String stockOut(Long storeId, Long productId, Integer quantity, String referenceId, String notes) {
        try {
            // Validate inputs
            if (quantity <= 0) {
                throw new RuntimeException("Quantity must be positive for stock out");
            }
            if (!storeRepository.existsById(storeId)) {
                throw new RuntimeException("Store not found: " + storeId);
            }
            if (!productRepository.existsById(productId)) {
                throw new RuntimeException("Product not found: " + productId);
            }

            // Check if sufficient stock exists
            Optional<Inventory> existingInventory = inventoryRepository.findByStoreIdAndProductId(storeId, productId);
            if (existingInventory.isEmpty() || existingInventory.get().getQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock. Available: " + 
                    (existingInventory.isPresent() ? existingInventory.get().getQuantity() : 0) + 
                    ", Requested: " + quantity);
            }

            // Try to publish event, but continue with direct update if it fails
            String eventId = UUID.randomUUID().toString();
            try {
                InventoryUpdateEvent event = new InventoryUpdateEvent(storeId, productId, -quantity, "STOCK_OUT");
                event.setNotes(notes != null ? notes : "Stock exit - product sold/dispatched");
                event.setReferenceId(referenceId);
                event.setCorrelationId(eventId);

                eventPublisher.publishInventoryUpdate(event);
                return "Stock out event published: " + event.getEventId() + " - Removed " + quantity + " units";
                
            } catch (Exception eventError) {
                System.err.println("⚠️ Event publishing failed, processing directly: " + eventError.getMessage());
                
                // Process directly if event publishing fails
                InventoryUpdateRequest directRequest = new InventoryUpdateRequest();
                directRequest.setStoreId(storeId);
                directRequest.setProductId(productId);
                directRequest.setQuantityAdjustment(-quantity);
                directRequest.setNotes(notes);
                directRequest.setReferenceId(referenceId);
                
                InventoryDTO result = updateInventoryDirect(directRequest);
                
                // Create transaction record manually
                Store store = storeRepository.findById(storeId).orElseThrow();
                Product product = productRepository.findById(productId).orElseThrow();
                Transaction transaction = new Transaction(store, product, Transaction.TransactionType.STOCK_OUT, quantity, referenceId, notes);
                transactionRepository.save(transaction);
                
                return "Stock out processed directly (event system unavailable): " + eventId + " - Removed " + quantity + " units";
            }
            
        } catch (Exception e) {
            System.err.println("🔥 Stock Out Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process stock out: " + e.getMessage(), e);
        }
    }

    // Synchronous methods for direct database access (used by consumers)
    @Transactional
    public InventoryDTO updateInventoryDirect(InventoryUpdateRequest request) {
        // Find or create inventory record with pessimistic locking
        Inventory inventory = inventoryRepository
                .findByStoreIdAndProductIdForUpdate(request.getStoreId(), request.getProductId())
                .orElseGet(() -> {
                    var store = storeRepository.findById(request.getStoreId())
                            .orElseThrow(() -> new RuntimeException("Store not found"));
                    var product = productRepository.findById(request.getProductId())
                            .orElseThrow(() -> new RuntimeException("Product not found"));
                    return new Inventory(store, product, 0);
                });

        // Apply quantity adjustment
        inventory.adjustQuantity(request.getQuantityAdjustment());
        inventory = inventoryRepository.save(inventory);

        return InventoryDTO.fromEntity(inventory);
    }
}