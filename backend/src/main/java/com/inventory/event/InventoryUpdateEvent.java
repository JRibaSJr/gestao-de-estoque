package com.inventory.event;

public class InventoryUpdateEvent extends InventoryEvent {
    
    private Integer quantityChange;
    private Integer newQuantity;
    private String operation; // ADD, SUBTRACT, SET
    private String notes;
    private String referenceId;
    private Long version; // For optimistic locking
    
    public InventoryUpdateEvent() {
        super();
        setEventType("INVENTORY_UPDATE");
        setPriority(10); // High priority
    }
    
    public InventoryUpdateEvent(Long storeId, Long productId, Integer quantityChange, String operation) {
        super("INVENTORY_UPDATE", storeId, productId);
        this.quantityChange = quantityChange;
        this.operation = operation;
        setPriority(10);
    }
    
    // Getters and Setters
    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }
    
    public Integer getNewQuantity() { return newQuantity; }
    public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }
    
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}