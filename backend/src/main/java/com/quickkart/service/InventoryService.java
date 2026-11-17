package com.quickkart.service;

import com.quickkart.dto.AdminDtos;
import com.quickkart.entity.*;
import com.quickkart.entity.enums.InventoryTransactionType;
import com.quickkart.entity.enums.ProductStatus;
import com.quickkart.exception.BadRequestException;
import com.quickkart.exception.InsufficientStockException;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.repository.InventoryRepository;
import com.quickkart.repository.InventoryTransactionRepository;
import com.quickkart.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public InventoryService(
            InventoryRepository inventoryRepository,
            InventoryTransactionRepository transactionRepository,
            ProductRepository productRepository,
            AuditLogService auditLogService) {
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.InventoryItemDto> getAllInventory() {
        return inventoryRepository.findAllActiveInventory().stream()
                .map(this::toInventoryItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.InventoryItemDto> getLowStockInventory() {
        return inventoryRepository.findLowStockInventory().stream()
                .map(this::toInventoryItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AdminDtos.InventoryTransactionDto> getTransactions(int page, int size) {
        return transactionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(tx -> new AdminDtos.InventoryTransactionDto(
                        tx.getId(),
                        tx.getVariant().getId(),
                        tx.getVariant().getSku(),
                        tx.getVariant().getProduct().getName(),
                        tx.getChangeAmount(),
                        tx.getPreviousQuantity(),
                        tx.getNewQuantity(),
                        tx.getTransactionType().name(),
                        tx.getNotes(),
                        tx.getCreatedAt()
                ));
    }

    /**
     * Updates stock quantity for a variant and automatically transitions the parent product
     * between ACTIVE and OUT_OF_STOCK based on total available stock.
     */
    @Transactional
    public AdminDtos.InventoryItemDto adjustVariantStock(Long variantId, AdminDtos.UpdateInventoryRequest req, Long actorUserId) {
        Inventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for variant ID: " + variantId));

        if (req.quantity() < inventory.getReservedQuantity()) {
            throw new BadRequestException("Stock quantity cannot be less than currently reserved quantity (" + inventory.getReservedQuantity() + ").");
        }

        int prev = inventory.getQuantity();
        int next = req.quantity();
        int diff = next - prev;

        inventory.setQuantity(next);
        if (req.lowStockThreshold() != null) {
            inventory.setLowStockThreshold(req.lowStockThreshold());
        }
        inventoryRepository.save(inventory);

        if (diff != 0) {
            InventoryTransactionType type = diff > 0 ? InventoryTransactionType.RESTOCK : InventoryTransactionType.MANUAL_ADJUSTMENT;
            recordTransaction(inventory.getVariant(), null, diff, prev, next, type,
                    req.notes() != null ? req.notes() : "Admin inventory update", actorUserId);
        }

        syncProductStockStatus(inventory.getVariant().getProduct());
        auditLogService.log("INVENTORY_UPDATED", "VARIANT", String.valueOf(variantId),
                "Stock changed from " + prev + " to " + next);
        return toInventoryItemDto(inventory);
    }

    /**
     * Immediately deducts stock using a pessimistic row lock (used for COD orders).
     */
    @Transactional
    public void deductStockForImmediateOrder(Order order, Long actorUserId) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByVariantIdForUpdate(item.getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for variant " + item.getVariant().getSku()));

            Product product = inventory.getVariant().getProduct();
            validateOrderable(product, inventory.getVariant(), inventory, item.getQuantity());

            int prev = inventory.getQuantity();
            int next = prev - item.getQuantity();
            inventory.setQuantity(next);
            inventoryRepository.save(inventory);

            product.setTotalSales(product.getTotalSales() + item.getQuantity());
            productRepository.save(product);

            recordTransaction(inventory.getVariant(), order, -item.getQuantity(), prev, next,
                    InventoryTransactionType.SALE, "Order " + order.getOrderNumber() + " placed (COD)", actorUserId);
            syncProductStockStatus(product);
        }
    }

    /**
     * Reserves stock using a pessimistic row lock while online payment is pending.
     */
    @Transactional
    public void reserveStockForPendingPayment(Order order) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByVariantIdForUpdate(item.getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for variant " + item.getVariant().getSku()));

            Product product = inventory.getVariant().getProduct();
            validateOrderable(product, inventory.getVariant(), inventory, item.getQuantity());

            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
            inventoryRepository.save(inventory);
            syncProductStockStatus(product);
        }
    }

    /**
     * Finalizes reserved stock into actual deduction upon payment verification.
     */
    @Transactional
    public void commitReservedStock(Order order, Long actorUserId) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByVariantIdForUpdate(item.getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for variant " + item.getVariant().getSku()));

            int prev = inventory.getQuantity();
            int next = Math.max(0, prev - item.getQuantity());
            int newReserved = Math.max(0, inventory.getReservedQuantity() - item.getQuantity());

            inventory.setQuantity(next);
            inventory.setReservedQuantity(newReserved);
            inventoryRepository.save(inventory);

            Product product = inventory.getVariant().getProduct();
            product.setTotalSales(product.getTotalSales() + item.getQuantity());
            productRepository.save(product);

            recordTransaction(inventory.getVariant(), order, -item.getQuantity(), prev, next,
                    InventoryTransactionType.SALE, "Order " + order.getOrderNumber() + " payment verified", actorUserId);
            syncProductStockStatus(product);
        }
    }

    /**
     * Releases reserved stock if payment fails or pending order is cancelled.
     */
    @Transactional
    public void releaseReservedStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByVariantIdForUpdate(item.getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for variant " + item.getVariant().getSku()));
            inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - item.getQuantity()));
            inventoryRepository.save(inventory);
            syncProductStockStatus(inventory.getVariant().getProduct());
        }
    }

    /**
     * Restores deducted stock when a confirmed/paid order is cancelled or returned.
     */
    @Transactional
    public void restoreDeductedStock(Order order, InventoryTransactionType txType, String notes, Long actorUserId) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByVariantIdForUpdate(item.getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for variant " + item.getVariant().getSku()));

            int prev = inventory.getQuantity();
            int next = prev + item.getQuantity();
            inventory.setQuantity(next);
            inventoryRepository.save(inventory);

            Product product = inventory.getVariant().getProduct();
            product.setTotalSales(Math.max(0L, product.getTotalSales() - item.getQuantity()));
            productRepository.save(product);

            recordTransaction(inventory.getVariant(), order, item.getQuantity(), prev, next, txType, notes, actorUserId);
            syncProductStockStatus(product);
        }
    }

    @Transactional
    public void syncProductStockStatus(Product product) {
        int totalAvailable = product.getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActive()) && v.getInventory() != null)
                .mapToInt(v -> v.getInventory().getAvailableQuantity())
                .sum();

        if (totalAvailable <= 0 && product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
            productRepository.save(product);
        } else if (totalAvailable > 0 && product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
            productRepository.save(product);
        }
    }

    public void recordTransaction(
            ProductVariant variant,
            Order order,
            int changeAmount,
            int previousQuantity,
            int newQuantity,
            InventoryTransactionType type,
            String notes,
            Long actorUserId) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setVariant(variant);
        tx.setOrder(order);
        tx.setChangeAmount(changeAmount);
        tx.setPreviousQuantity(previousQuantity);
        tx.setNewQuantity(newQuantity);
        tx.setTransactionType(type);
        tx.setNotes(notes);
        tx.setPerformedByUserId(actorUserId);
        transactionRepository.save(tx);
    }

    private void validateOrderable(Product product, ProductVariant variant, Inventory inventory, int requestedQty) {
        if (Boolean.TRUE.equals(product.getDeleted()) || product.getStatus() == ProductStatus.INACTIVE || product.getStatus() == ProductStatus.DRAFT) {
            throw new InsufficientStockException("Product '" + product.getName() + "' is currently unavailable.");
        }
        if (!Boolean.TRUE.equals(variant.getActive())) {
            throw new InsufficientStockException("Selected option (" + variant.getSize() + " / " + variant.getColor() + ") for '" + product.getName() + "' is unavailable.");
        }
        int available = inventory.getAvailableQuantity();
        if (available < requestedQty) {
            throw new InsufficientStockException("Insufficient stock for '" + product.getName() + "' (" + variant.getSize() + " / " + variant.getColor() + "). Available: " + available);
        }
    }

    private AdminDtos.InventoryItemDto toInventoryItemDto(Inventory inv) {
        ProductVariant v = inv.getVariant();
        Product p = v.getProduct();
        int available = inv.getAvailableQuantity();
        return new AdminDtos.InventoryItemDto(
                inv.getId(),
                v.getId(),
                p.getId(),
                p.getName(),
                p.getBrand().getName(),
                p.getCategory().getName(),
                p.getStatus().name(),
                v.getSku(),
                v.getSize(),
                v.getColor(),
                inv.getQuantity(),
                inv.getReservedQuantity(),
                available,
                inv.getLowStockThreshold(),
                available > 0 && available <= inv.getLowStockThreshold(),
                available <= 0,
                inv.getUpdatedAt()
        );
    }
}
