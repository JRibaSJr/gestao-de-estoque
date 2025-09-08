package com.inventory.integration;

import com.inventory.dto.InventoryDTO;
import com.inventory.model.Inventory;
import com.inventory.model.Store;
import com.inventory.model.Product;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.StoreRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:test-inventory.db",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.rabbitmq.host=localhost",
    "spring.data.redis.host=localhost"
})
@Transactional
class InventoryIntegrationTest {

    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private InventoryService inventoryService;

    private Store testStore;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create test store
        testStore = new Store();
        testStore.setName("Integration Test Store");
        testStore.setLocation("Test Location");
        testStore.setStatus(Store.StoreStatus.ACTIVE);
        testStore = storeRepository.save(testStore);

        // Create test product
        testProduct = new Product();
        testProduct.setName("Integration Test Product");
        testProduct.setSku("INT-TEST-001");
        testProduct.setCategory("Test Category");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(10.99);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void testOptimisticLocking_ConcurrentUpdates() throws InterruptedException {
        // Create initial inventory
        Inventory inventory = new Inventory(testStore, testProduct, 100);
        inventory = inventoryRepository.save(inventory);
        
        final Long inventoryId = inventory.getId();
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger exceptionCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Simulate two concurrent updates
        for (int i = 0; i < 2; i++) {
            final int adjustment = (i == 0) ? 10 : -5;
            executor.submit(() -> {
                try {
                    Inventory inv = inventoryRepository.findById(inventoryId).orElseThrow();
                    
                    // Simulate processing time
                    Thread.sleep(100);
                    
                    inv.adjustQuantity(adjustment);
                    inventoryRepository.save(inv);
                    successCount.incrementAndGet();
                    
                } catch (OptimisticLockException | IllegalArgumentException e) {
                    exceptionCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // One should succeed, one should fail due to optimistic locking
        assertEquals(1, successCount.get());
        assertEquals(1, exceptionCount.get());
    }

    @Test
    void testInventoryPersistence_CRUD() {
        // Create
        Inventory inventory = new Inventory(testStore, testProduct, 50);
        inventory = inventoryRepository.save(inventory);
        assertNotNull(inventory.getId());
        assertEquals(1L, inventory.getVersion());

        // Read
        Inventory found = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertEquals(50, found.getQuantity());
        assertEquals(testStore.getId(), found.getStore().getId());

        // Update
        found.adjustQuantity(25);
        found = inventoryRepository.save(found);
        assertEquals(75, found.getQuantity());
        assertEquals(2L, found.getVersion());

        // Delete
        inventoryRepository.delete(found);
        assertFalse(inventoryRepository.findById(inventory.getId()).isPresent());
    }

    @Test
    void testInventoryConstraints_UniqueStoreProduct() {
        // Create first inventory
        Inventory inventory1 = new Inventory(testStore, testProduct, 30);
        inventoryRepository.save(inventory1);

        // Try to create duplicate - should fail
        Inventory inventory2 = new Inventory(testStore, testProduct, 40);
        
        assertThrows(Exception.class, () -> {
            inventoryRepository.save(inventory2);
            inventoryRepository.flush(); // Force immediate persistence
        });
    }

    @Test
    void testLowStockQuery() {
        // Create multiple inventory records
        Inventory highStock = new Inventory(testStore, testProduct, 100);
        inventoryRepository.save(highStock);
        
        Product lowStockProduct = new Product();
        lowStockProduct.setName("Low Stock Product");
        lowStockProduct.setSku("LOW-STOCK-001");
        lowStockProduct.setCategory("Test");
        lowStockProduct.setPrice(new BigDecimal("5.99"));
        lowStockProduct = productRepository.save(lowStockProduct);
        
        Inventory lowStock = new Inventory(testStore, lowStockProduct, 5);
        inventoryRepository.save(lowStock);

        // Test low stock query
        var lowStockItems = inventoryRepository.findLowStockItems(10);
        assertEquals(1, lowStockItems.size());
        assertEquals(5, lowStockItems.get(0).getQuantity());
    }

    @Test
    void testReservationLogic() {
        Inventory inventory = new Inventory(testStore, testProduct, 100);
        inventory = inventoryRepository.save(inventory);

        // Test reservation
        inventory.reserve(30);
        assertEquals(70, inventory.getAvailableQuantity());
        assertEquals(30, inventory.getReservedQuantity());

        // Test release reservation
        inventory.releaseReservation(10);
        assertEquals(80, inventory.getAvailableQuantity());
        assertEquals(20, inventory.getReservedQuantity());

        // Test over-reservation
        assertThrows(IllegalArgumentException.class, () -> inventory.reserve(85));
        
        // Test over-release
        assertThrows(IllegalArgumentException.class, () -> inventory.releaseReservation(25));
    }
}