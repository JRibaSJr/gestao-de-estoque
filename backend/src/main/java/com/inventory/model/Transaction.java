package com.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    
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
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    
    @NotNull
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "reference_id")
    private String referenceId; // For tracking related transactions
    
    @Column(length = 500)
    private String notes;
    
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    // Constructors
    public Transaction() {}
    
    public Transaction(Store store, Product product, TransactionType type, Integer quantity) {
        this.store = store;
        this.product = product;
        this.type = type;
        this.quantity = quantity;
    }
    
    public Transaction(Store store, Product product, TransactionType type, Integer quantity, String referenceId, String notes) {
        this(store, product, type, quantity);
        this.referenceId = referenceId;
        this.notes = notes;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public enum TransactionType {
        STOCK_IN,     // Entrada de estoque
        STOCK_OUT,    // Saída de estoque (venda)
        TRANSFER_OUT, // Transferência para outra loja
        TRANSFER_IN,  // Recebimento de transferência
        ADJUSTMENT,   // Ajuste de estoque
        RESERVATION,  // Reserva de estoque
        RELEASE       // Liberação de reserva
    }
}