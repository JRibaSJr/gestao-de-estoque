package com.inventory.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = InventoryUpdateEvent.class, name = "INVENTORY_UPDATE"),
    @JsonSubTypes.Type(value = InventoryTransferEvent.class, name = "INVENTORY_TRANSFER"),
    @JsonSubTypes.Type(value = InventorySyncEvent.class, name = "INVENTORY_SYNC"),
    @JsonSubTypes.Type(value = InventoryAuditEvent.class, name = "INVENTORY_AUDIT")
})
public abstract class InventoryEvent {
    
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Long storeId;
    private Long productId;
    private String correlationId;
    private Integer priority;
    
    public InventoryEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.priority = 5; // Default priority
    }
    
    public InventoryEvent(String eventType, Long storeId, Long productId) {
        this();
        this.eventType = eventType;
        this.storeId = storeId;
        this.productId = productId;
    }
    
    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}