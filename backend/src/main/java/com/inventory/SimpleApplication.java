package com.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class SimpleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleApplication.class, args);
    }

    // CORS já configurado em CorsConfig.java

    @GetMapping("/stores")
    public List<Map<String, Object>> getStores() {
        return List.of(
            Map.of("id", 1, "name", "Loja Centro SP", "location", "São Paulo - Centro", "status", "ACTIVE"),
            Map.of("id", 2, "name", "Shopping Vila Olímpia", "location", "São Paulo - Vila Olímpia", "status", "ACTIVE"),
            Map.of("id", 3, "name", "Loja Santana", "location", "São Paulo - Santana", "status", "ACTIVE"),
            Map.of("id", 4, "name", "Loja Rio Copacabana", "location", "Rio de Janeiro - Copacabana", "status", "ACTIVE"),
            Map.of("id", 5, "name", "Shopping Barra RJ", "location", "Rio de Janeiro - Barra da Tijuca", "status", "ACTIVE")
        );
    }

    @GetMapping("/products")
    public List<Map<String, Object>> getProducts() {
        return List.of(
            Map.of("id", 1, "name", "iPhone 15 Pro", "description", "Apple iPhone 15 Pro 256GB", "category", "Smartphones", "price", 4999.99, "sku", "IPHONE15-PRO-256"),
            Map.of("id", 2, "name", "Samsung Galaxy S24", "description", "Samsung Galaxy S24 256GB", "category", "Smartphones", "price", 3899.99, "sku", "GALAXY-S24-256"),
            Map.of("id", 3, "name", "MacBook Air M2", "description", "Apple MacBook Air M2 256GB", "category", "Notebooks", "price", 8999.99, "sku", "MBA-M2-256"),
            Map.of("id", 4, "name", "iPad Pro 12.9", "description", "Apple iPad Pro 12.9 M2 256GB", "category", "Tablets", "price", 7499.99, "sku", "IPAD-PRO-129-256"),
            Map.of("id", 5, "name", "AirPods Pro 2", "description", "Apple AirPods Pro 2ª Geração", "category", "Acessórios", "price", 1899.99, "sku", "AIRPODS-PRO-2")
        );
    }

    @GetMapping("/inventory")
    public List<Map<String, Object>> getInventory() {
        return List.of(
            Map.of("id", 1, "store", Map.of("id", 1, "name", "Loja Centro SP"), "product", Map.of("id", 1, "name", "iPhone 15 Pro", "price", 4999.99, "sku", "IPHONE15-PRO-256"), "quantity", 25, "version", 1),
            Map.of("id", 2, "store", Map.of("id", 1, "name", "Loja Centro SP"), "product", Map.of("id", 2, "name", "Samsung Galaxy S24", "price", 3899.99, "sku", "GALAXY-S24-256"), "quantity", 15, "version", 1),
            Map.of("id", 3, "store", Map.of("id", 1, "name", "Loja Centro SP"), "product", Map.of("id", 3, "name", "MacBook Air M2", "price", 8999.99, "sku", "MBA-M2-256"), "quantity", 8, "version", 1),
            Map.of("id", 4, "store", Map.of("id", 2, "name", "Shopping Vila Olímpia"), "product", Map.of("id", 1, "name", "iPhone 15 Pro", "price", 4999.99, "sku", "IPHONE15-PRO-256"), "quantity", 18, "version", 1),
            Map.of("id", 5, "store", Map.of("id", 2, "name", "Shopping Vila Olímpia"), "product", Map.of("id", 4, "name", "iPad Pro 12.9", "price", 7499.99, "sku", "IPAD-PRO-129-256"), "quantity", 5, "version", 1),
            Map.of("id", 6, "store", Map.of("id", 3, "name", "Loja Santana"), "product", Map.of("id", 5, "name", "AirPods Pro 2", "price", 1899.99, "sku", "AIRPODS-PRO-2"), "quantity", 3, "version", 1),
            Map.of("id", 7, "store", Map.of("id", 4, "name", "Loja Rio Copacabana"), "product", Map.of("id", 1, "name", "iPhone 15 Pro", "price", 4999.99, "sku", "IPHONE15-PRO-256"), "quantity", 12, "version", 1),
            Map.of("id", 8, "store", Map.of("id", 5, "name", "Shopping Barra RJ"), "product", Map.of("id", 2, "name", "Samsung Galaxy S24", "price", 3899.99, "sku", "GALAXY-S24-256"), "quantity", 9, "version", 1)
        );
    }

    @GetMapping("/inventory/low-stock")
    public List<Map<String, Object>> getLowStock() {
        return List.of(
            Map.of("id", 5, "store", Map.of("id", 2, "name", "Shopping Vila Olímpia"), "product", Map.of("id", 4, "name", "iPad Pro 12.9", "price", 7499.99, "sku", "IPAD-PRO-129-256"), "quantity", 5, "version", 1),
            Map.of("id", 6, "store", Map.of("id", 3, "name", "Loja Santana"), "product", Map.of("id", 5, "name", "AirPods Pro 2", "price", 1899.99, "sku", "AIRPODS-PRO-2"), "quantity", 3, "version", 1),
            Map.of("id", 8, "store", Map.of("id", 5, "name", "Shopping Barra RJ"), "product", Map.of("id", 2, "name", "Samsung Galaxy S24", "price", 3899.99, "sku", "GALAXY-S24-256"), "quantity", 9, "version", 1)
        );
    }

    @PostMapping("/inventory/stock-in")
    public Map<String, String> stockIn(@RequestBody Map<String, Object> data) {
        return Map.of("message", "Stock in realizado com sucesso: " + data.get("quantity") + " unidades adicionadas");
    }

    @PostMapping("/inventory/stock-out")
    public Map<String, String> stockOut(@RequestBody Map<String, Object> data) {
        return Map.of("message", "Stock out realizado com sucesso: " + data.get("quantity") + " unidades removidas");
    }
}