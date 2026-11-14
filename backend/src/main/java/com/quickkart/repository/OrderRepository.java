package com.quickkart.repository;

import com.quickkart.entity.Order;
import com.quickkart.entity.enums.OrderStatus;
import com.quickkart.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Order> findTop10ByOrderByCreatedAtDesc();

    long countByOrderStatus(OrderStatus status);
    long countByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus <> 'CANCELLED' AND o.paymentStatus <> 'FAILED' AND o.paymentStatus <> 'REFUNDED'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user.id = :userId AND o.orderStatus <> 'CANCELLED'")
    BigDecimal sumUserTotalSpent(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :start AND o.createdAt <= :end AND o.orderStatus <> 'CANCELLED' AND o.paymentStatus <> 'FAILED'")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT o FROM Order o WHERE o.createdAt >= :start AND o.orderStatus <> 'CANCELLED' AND o.paymentStatus <> 'FAILED' ORDER BY o.createdAt ASC")
    List<Order> findValidOrdersSince(@Param("start") LocalDateTime start);

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items i WHERE o.user.id = :userId AND i.product.id = :productId AND o.orderStatus <> 'CANCELLED'")
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT o FROM Order o JOIN o.items i WHERE o.user.id = :userId AND i.product.id = :productId AND o.orderStatus <> 'CANCELLED' ORDER BY o.createdAt DESC")
    List<Order> findUserOrdersForProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
