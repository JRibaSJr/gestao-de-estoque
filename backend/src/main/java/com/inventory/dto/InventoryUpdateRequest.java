package com.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class InventoryUpdateRequest {
    
    @NotNull(message = "Store ID is required")
    private Long storeId;
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Quantity adjustment is required")
    private Integer quantityAdjustment;
    
    private String notes;
    private String referenceId;
    
    // For optimistic locking
    private Long version;
    
    // Constructors
    public InventoryUpdateRequest() {}
    
    public InventoryUpdateRequest(Long storeId, Long productId, Integer quantityAdjustment) {
        this.storeId = storeId;
        this.productId = productId;
        this.quantityAdjustment = quantityAdjustment;
    }
    
    // Getters and Setters
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantityAdjustment() { return quantityAdjustment; }
    public void setQuantityAdjustment(Integer quantityAdjustment) { this.quantityAdjustment = quantityAdjustment; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}