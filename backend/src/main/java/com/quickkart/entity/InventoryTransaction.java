package com.quickkart.entity;

import com.quickkart.entity.enums.InventoryTransactionType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount;

    @Column(name = "previous_quantity", nullable = false)
    private Integer previousQuantity;

    @Column(name = "new_quantity", nullable = false)
    private Integer newQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private InventoryTransactionType transactionType;

    @Column(length = 500)
    private String notes;

    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Integer getChangeAmount() { return changeAmount; }
    public void setChangeAmount(Integer changeAmount) { this.changeAmount = changeAmount; }
    public Integer getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(Integer previousQuantity) { this.previousQuantity = previousQuantity; }
    public Integer getNewQuantity() { return newQuantity; }
    public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }
    public InventoryTransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(InventoryTransactionType transactionType) { this.transactionType = transactionType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getPerformedByUserId() { return performedByUserId; }
    public void setPerformedByUserId(Long performedByUserId) { this.performedByUserId = performedByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
