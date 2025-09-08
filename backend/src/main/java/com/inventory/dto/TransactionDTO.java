package com.inventory.dto;

import com.inventory.model.Transaction;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class TransactionDTO {
    
    private Long id;
    
    @NotNull
    private Long storeId;
    
    @NotNull
    private Long productId;
    
    @NotNull
    private Transaction.TransactionType type;
    
    @NotNull
    private Integer quantity;
    
    private String referenceId;
    private String notes;
    private LocalDateTime timestamp;
    
    // Additional fields for UI
    private String storeName;
    private String storeLocation;
    private String productName;
    private String productSku;
    
    // Constructors
    public TransactionDTO() {}
    
    public TransactionDTO(Long storeId, Long productId, Transaction.TransactionType type, Integer quantity) {
        this.storeId = storeId;
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
    }
    
    // Static factory methods
    public static TransactionDTO fromEntity(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setStoreId(transaction.getStore().getId());
        dto.setProductId(transaction.getProduct().getId());
        dto.setType(transaction.getType());
        dto.setQuantity(transaction.getQuantity());
        dto.setReferenceId(transaction.getReferenceId());
        dto.setNotes(transaction.getNotes());
        dto.setTimestamp(transaction.getTimestamp());
        
        // Set additional fields
        dto.setStoreName(transaction.getStore().getName());
        dto.setStoreLocation(transaction.getStore().getLocation());
        dto.setProductName(transaction.getProduct().getName());
        dto.setProductSku(transaction.getProduct().getSku());
        
        return dto;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Transaction.TransactionType getType() { return type; }
    public void setType(Transaction.TransactionType type) { this.type = type; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    
    public String getStoreLocation() { return storeLocation; }
    public void setStoreLocation(String storeLocation) { this.storeLocation = storeLocation; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
}