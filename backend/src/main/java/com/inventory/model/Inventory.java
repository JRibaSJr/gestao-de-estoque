package com.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "product_id"}))
public class Inventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Min(value = 0, message = "Quantity cannot be negative")
    @Column(nullable = false)
    private Integer quantity = 0;
    
    @Min(value = 0, message = "Reserved quantity cannot be negative")
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;
    
    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Version
    private Long version; // For optimistic locking
    
    // Constructors
    public Inventory() {}
    
    public Inventory(Store store, Product product, Integer quantity) {
        this.store = store;
        this.product = product;
        this.quantity = quantity;
    }
    
    // Business methods
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
    
    public boolean canReserve(Integer amount) {
        return getAvailableQuantity() >= amount;
    }
    
    public void reserve(Integer amount) {
        if (!canReserve(amount)) {
            throw new IllegalArgumentException("Insufficient available quantity to reserve");
        }
        this.reservedQuantity += amount;
    }
    
    public void releaseReservation(Integer amount) {
        if (reservedQuantity < amount) {
            throw new IllegalArgumentException("Cannot release more than reserved");
        }
        this.reservedQuantity -= amount;
    }
    
    public void adjustQuantity(Integer adjustment) {
        int newQuantity = this.quantity + adjustment;
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (newQuantity < reservedQuantity) {
            throw new IllegalArgumentException("Cannot reduce quantity below reserved amount");
        }
        this.quantity = newQuantity;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}