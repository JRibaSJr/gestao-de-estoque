package com.inventory.config;

import com.inventory.model.Store;
import com.inventory.model.Product;
import com.inventory.model.Inventory;
import com.inventory.repository.StoreRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private InventoryRepository inventoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (storeRepository.count() == 0) {
            initializeSampleData();
        }
    }

    private void initializeSampleData() {
        // Create Stores
        Store store1 = new Store("Loja Centro", "São Paulo - Centro");
        store1.setStatus(Store.StoreStatus.ACTIVE);
        store1.setLastSync(LocalDateTime.now().minusMinutes(30));
        
        Store store2 = new Store("Loja Shopping", "São Paulo - Vila Olímpia");
        store2.setStatus(Store.StoreStatus.ACTIVE);
        store2.setLastSync(LocalDateTime.now().minusMinutes(45));
        
        Store store3 = new Store("Loja Norte", "São Paulo - Santana");
        store3.setStatus(Store.StoreStatus.MAINTENANCE);
        store3.setLastSync(LocalDateTime.now().minusHours(2));
        
        storeRepository.save(store1);
        storeRepository.save(store2);
        storeRepository.save(store3);
        
        // Create Products
        Product product1 = new Product("iPhone 15", "Smartphone Apple iPhone 15 128GB", 
                                     "Eletrônicos", new BigDecimal("4299.99"), "IPHONE15-128");
        
        Product product2 = new Product("Samsung Galaxy S24", "Smartphone Samsung Galaxy S24 256GB", 
                                     "Eletrônicos", new BigDecimal("3899.99"), "GALAXY-S24-256");
        
        Product product3 = new Product("MacBook Air M2", "Notebook Apple MacBook Air M2 8GB 256GB", 
                                     "Computadores", new BigDecimal("8999.99"), "MACBOOK-AIR-M2");
        
        Product product4 = new Product("Dell XPS 13", "Notebook Dell XPS 13 16GB 512GB", 
                                     "Computadores", new BigDecimal("6799.99"), "DELL-XPS13");
        
        Product product5 = new Product("AirPods Pro", "Fone Apple AirPods Pro 2ª Geração", 
                                     "Acessórios", new BigDecimal("1899.99"), "AIRPODS-PRO-2");
        
        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);
        productRepository.save(product4);
        productRepository.save(product5);
        
        // Create Inventory records
        // Store 1 inventory
        inventoryRepository.save(new Inventory(store1, product1, 25));
        inventoryRepository.save(new Inventory(store1, product2, 15));
        inventoryRepository.save(new Inventory(store1, product3, 8));
        inventoryRepository.save(new Inventory(store1, product4, 12));
        inventoryRepository.save(new Inventory(store1, product5, 30));
        
        // Store 2 inventory  
        inventoryRepository.save(new Inventory(store2, product1, 18));
        inventoryRepository.save(new Inventory(store2, product2, 22));
        inventoryRepository.save(new Inventory(store2, product3, 5));
        inventoryRepository.save(new Inventory(store2, product4, 9));
        inventoryRepository.save(new Inventory(store2, product5, 40));
        
        // Store 3 inventory
        inventoryRepository.save(new Inventory(store3, product1, 10));
        inventoryRepository.save(new Inventory(store3, product2, 8));
        inventoryRepository.save(new Inventory(store3, product3, 3));
        inventoryRepository.save(new Inventory(store3, product4, 6));
        inventoryRepository.save(new Inventory(store3, product5, 20));
        
        System.out.println("✅ Sample data initialized successfully!");
        System.out.println("📊 Created 3 stores, 5 products, and 15 inventory records");
    }
}