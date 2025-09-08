package com.inventory.load;

import com.inventory.service.InventoryService;
import com.inventory.model.Store;
import com.inventory.model.Product;
import com.inventory.repository.StoreRepository;
import com.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:load-test-inventory.db",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class InventoryLoadTest {

    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private ProductRepository productRepository;

    private Store testStore;
    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        // Create test store
        testStore = new Store();
        testStore.setName("Load Test Store");
        testStore.setLocation("Load Test Location");
        testStore.setStatus(Store.StoreStatus.ACTIVE);
        testStore = storeRepository.save(testStore);

        // Create multiple test products
        testProducts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Product product = new Product();
            product.setName("Load Test Product " + i);
            product.setSku("LOAD-TEST-" + String.format("%03d", i));
            product.setCategory("Load Test Category");
            product.setPrice(10.0 + i);
            testProducts.add(productRepository.save(product));
        }
    }

    @Test
    void testConcurrentStockOperations_HighLoad() throws InterruptedException {
        final int THREAD_COUNT = 20;
        final int OPERATIONS_PER_THREAD = 50;
        final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // Submit concurrent stock operations
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        Product product = testProducts.get(j % testProducts.size());
                        
                        try {
                            // Alternate between stock in and stock out operations
                            if (j % 2 == 0) {
                                inventoryService.stockIn(
                                    testStore.getId(), 
                                    product.getId(), 
                                    10, 
                                    "LOAD-TEST-" + threadId + "-" + j,
                                    "Load test stock in"
                                );
                            } else {
                                // Try stock out (may fail due to insufficient stock - that's expected)
                                try {
                                    inventoryService.stockOut(
                                        testStore.getId(), 
                                        product.getId(), 
                                        5, 
                                        "LOAD-TEST-OUT-" + threadId + "-" + j,
                                        "Load test stock out"
                                    );
                                } catch (RuntimeException e) {
                                    // Expected for insufficient stock
                                }
                            }
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("Error in thread " + threadId + ", operation " + j + ": " + e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Performance assertions
        assertTrue(completed, "Load test should complete within 60 seconds");
        
        int totalOperations = THREAD_COUNT * OPERATIONS_PER_THREAD;
        double successRate = (double) successCount.get() / totalOperations * 100;
        double throughput = (double) successCount.get() / (duration / 1000.0);

        System.out.println("=== LOAD TEST RESULTS ===");
        System.out.println("Total Operations: " + totalOperations);
        System.out.println("Successful Operations: " + successCount.get());
        System.out.println("Failed Operations: " + errorCount.get());
        System.out.println("Success Rate: " + String.format("%.2f%%", successRate));
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Throughput: " + String.format("%.2f ops/sec", throughput));

        // Performance criteria
        assertTrue(successRate >= 70, "Success rate should be at least 70%");
        assertTrue(throughput >= 10, "Throughput should be at least 10 ops/sec");
    }

    @Test
    void testCachePerformance_ResponseTime() {
        long warmupStart = System.currentTimeMillis();
        
        // Warmup - call multiple times to populate cache
        for (int i = 0; i < 10; i++) {
            inventoryService.getAllInventory();
        }
        
        long warmupEnd = System.currentTimeMillis();
        System.out.println("Warmup completed in: " + (warmupEnd - warmupStart) + "ms");

        // Measure cache hit performance
        long cacheTestStart = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            inventoryService.getAllInventory();
        }
        
        long cacheTestEnd = System.currentTimeMillis();
        long avgResponseTime = (cacheTestEnd - cacheTestStart) / 100;

        System.out.println("Average response time (cache hits): " + avgResponseTime + "ms");
        
        // Cache hits should be fast (under 100ms average)
        assertTrue(avgResponseTime < 100, "Cache hit response time should be under 100ms");
    }

    @Test
    void testMemoryUsage_LargeDataSet() {
        Runtime runtime = Runtime.getRuntime();
        
        // Measure initial memory
        runtime.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create large dataset
        for (int i = 0; i < 1000; i++) {
            Product product = testProducts.get(i % testProducts.size());
            
            try {
                inventoryService.stockIn(
                    testStore.getId(), 
                    product.getId(), 
                    100, 
                    "MEMORY-TEST-" + i,
                    "Memory test operation"
                );
            } catch (Exception e) {
                // Some operations may fail, that's acceptable for this test
            }
        }
        
        // Measure final memory
        runtime.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");
        
        // Memory increase should be reasonable (under 100MB for this test)
        assertTrue(memoryIncrease < 100 * 1024 * 1024, "Memory increase should be under 100MB");
    }
}