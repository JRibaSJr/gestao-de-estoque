package com.inventory.event;

public class InventoryAuditEvent extends InventoryEvent {
    
    private String action; // CREATE, UPDATE, DELETE, TRANSFER
    private Object oldValue;
    private Object newValue;
    private String userId;
    private String ipAddress;
    
    public InventoryAuditEvent() {
        super();
        setEventType("INVENTORY_AUDIT");
        setPriority(1); // Low priority but persistent
    }
    
    public InventoryAuditEvent(String action, Long storeId, Long productId, Object oldValue, Object newValue) {
        super("INVENTORY_AUDIT", storeId, productId);
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        setPriority(1);
    }
    
    // Getters and Setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public Object getOldValue() { return oldValue; }
    public void setOldValue(Object oldValue) { this.oldValue = oldValue; }
    
    public Object getNewValue() { return newValue; }
    public void setNewValue(Object newValue) { this.newValue = newValue; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}