package com.quickkart.repository;

import com.quickkart.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByVariantId(Long variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.variant.id = :variantId")
    Optional<Inventory> findByVariantIdForUpdate(@Param("variantId") Long variantId);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.variant v JOIN FETCH v.product p WHERE p.deleted = false AND (i.quantity - i.reservedQuantity) <= i.lowStockThreshold ORDER BY (i.quantity - i.reservedQuantity) ASC")
    List<Inventory> findLowStockInventory();

    @Query("SELECT i FROM Inventory i JOIN FETCH i.variant v JOIN FETCH v.product p JOIN FETCH p.brand JOIN FETCH p.category WHERE p.deleted = false ORDER BY (i.quantity - i.reservedQuantity) ASC")
    List<Inventory> findAllActiveInventory();
}
