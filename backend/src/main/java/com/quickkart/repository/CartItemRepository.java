package com.quickkart.repository;

import com.quickkart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);
    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);
}
