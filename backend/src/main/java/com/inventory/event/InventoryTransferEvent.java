package com.inventory.event;

public class InventoryTransferEvent extends InventoryEvent {
    
    private Long fromStoreId;
    private Long toStoreId;
    private Integer quantity;
    private String transferType; // START, RESERVE, CONFIRM, ROLLBACK
    private String sagaId; // For saga orchestration
    private String notes;
    
    public InventoryTransferEvent() {
        super();
        setEventType("INVENTORY_TRANSFER");
        setPriority(15); // Critical priority
    }
    
    public InventoryTransferEvent(Long fromStoreId, Long toStoreId, Long productId, Integer quantity, String transferType) {
        super("INVENTORY_TRANSFER", fromStoreId, productId);
        this.fromStoreId = fromStoreId;
        this.toStoreId = toStoreId;
        this.quantity = quantity;
        this.transferType = transferType;
        setPriority(15);
    }
    
    // Getters and Setters
    public Long getFromStoreId() { return fromStoreId; }
    public void setFromStoreId(Long fromStoreId) { this.fromStoreId = fromStoreId; }
    
    public Long getToStoreId() { return toStoreId; }
    public void setToStoreId(Long toStoreId) { this.toStoreId = toStoreId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public String getTransferType() { return transferType; }
    public void setTransferType(String transferType) { this.transferType = transferType; }
    
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}