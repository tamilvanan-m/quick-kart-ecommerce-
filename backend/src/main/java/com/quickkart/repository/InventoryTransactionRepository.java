package com.quickkart.repository;

import com.quickkart.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    Page<InventoryTransaction> findByVariantIdOrderByCreatedAtDesc(Long variantId, Pageable pageable);
    Page<InventoryTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
