package com.inventory.dto;

import com.inventory.model.Inventory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class InventoryDTO {
    
    private Long id;
    
    @NotNull
    private Long storeId;
    
    @NotNull 
    private Long productId;
    
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;
    
    @Min(value = 0, message = "Reserved quantity cannot be negative")
    private Integer reservedQuantity;
    
    private LocalDateTime lastUpdated;
    private Long version;
    
    // Additional fields for UI
    private String storeName;
    private String storeLocation;
    private String productName;
    private String productSku;
    private String productCategory;
    private Integer availableQuantity;
    
    // Constructors
    public InventoryDTO() {}
    
    public InventoryDTO(Long storeId, Long productId, Integer quantity) {
        this.storeId = storeId;
        this.productId = productId;
        this.quantity = quantity;
        this.reservedQuantity = 0;
    }
    
    // Static factory methods
    public static InventoryDTO fromEntity(Inventory inventory) {
        InventoryDTO dto = new InventoryDTO();
        dto.setId(inventory.getId());
        dto.setStoreId(inventory.getStore().getId());
        dto.setProductId(inventory.getProduct().getId());
        dto.setQuantity(inventory.getQuantity());
        dto.setReservedQuantity(inventory.getReservedQuantity());
        dto.setLastUpdated(inventory.getLastUpdated());
        dto.setVersion(inventory.getVersion());
        dto.setAvailableQuantity(inventory.getAvailableQuantity());
        
        // Set additional fields
        dto.setStoreName(inventory.getStore().getName());
        dto.setStoreLocation(inventory.getStore().getLocation());
        dto.setProductName(inventory.getProduct().getName());
        dto.setProductSku(inventory.getProduct().getSku());
        dto.setProductCategory(inventory.getProduct().getCategory());
        
        return dto;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    
    public String getStoreLocation() { return storeLocation; }
    public void setStoreLocation(String storeLocation) { this.storeLocation = storeLocation; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
    
    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }
    
    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
}