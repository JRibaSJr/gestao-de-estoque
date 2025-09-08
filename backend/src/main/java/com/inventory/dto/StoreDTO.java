package com.inventory.dto;

import com.inventory.model.Store;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class StoreDTO {
    
    private Long id;
    
    @NotBlank(message = "Store name is required")
    private String name;
    
    @NotBlank(message = "Location is required")
    private String location;
    
    @NotNull
    private Store.StoreStatus status;
    
    private LocalDateTime lastSync;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long totalProducts;
    private Long totalQuantity;
    
    // Constructors
    public StoreDTO() {}
    
    public StoreDTO(Long id, String name, String location, Store.StoreStatus status) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.status = status;
    }
    
    // Static factory methods
    public static StoreDTO fromEntity(Store store) {
        StoreDTO dto = new StoreDTO();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setLocation(store.getLocation());
        dto.setStatus(store.getStatus());
        dto.setLastSync(store.getLastSync());
        dto.setCreatedAt(store.getCreatedAt());
        dto.setUpdatedAt(store.getUpdatedAt());
        return dto;
    }
    
    public Store toEntity() {
        Store store = new Store();
        store.setId(this.id);
        store.setName(this.name);
        store.setLocation(this.location);
        store.setStatus(this.status);
        store.setLastSync(this.lastSync);
        return store;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public Store.StoreStatus getStatus() { return status; }
    public void setStatus(Store.StoreStatus status) { this.status = status; }
    
    public LocalDateTime getLastSync() { return lastSync; }
    public void setLastSync(LocalDateTime lastSync) { this.lastSync = lastSync; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(Long totalProducts) { this.totalProducts = totalProducts; }
    
    public Long getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Long totalQuantity) { this.totalQuantity = totalQuantity; }
}