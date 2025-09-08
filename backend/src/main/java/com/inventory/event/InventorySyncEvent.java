package com.inventory.event;

import java.util.Map;

public class InventorySyncEvent extends InventoryEvent {
    
    private String syncType; // FULL, DELTA, HEALTH_CHECK
    private Map<String, Object> syncData;
    private String targetStore; // "ALL" or specific store
    
    public InventorySyncEvent() {
        super();
        setEventType("INVENTORY_SYNC");
        setPriority(5); // Medium priority
    }
    
    public InventorySyncEvent(String syncType, Long storeId) {
        super("INVENTORY_SYNC", storeId, null);
        this.syncType = syncType;
        setPriority(5);
    }
    
    // Getters and Setters
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    
    public Map<String, Object> getSyncData() { return syncData; }
    public void setSyncData(Map<String, Object> syncData) { this.syncData = syncData; }
    
    public String getTargetStore() { return targetStore; }
    public void setTargetStore(String targetStore) { this.targetStore = targetStore; }
}