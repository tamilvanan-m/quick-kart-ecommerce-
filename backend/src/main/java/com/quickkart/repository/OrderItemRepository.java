package com.quickkart.repository;

import com.quickkart.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByProductId(Long productId);

    @Query("SELECT oi.product.id, oi.productName, oi.brandName, SUM(oi.quantity), SUM(oi.totalPrice) FROM OrderItem oi WHERE oi.order.orderStatus <> 'CANCELLED' GROUP BY oi.product.id, oi.productName, oi.brandName ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProducts();

    @Query("SELECT oi.product.category.name, SUM(oi.totalPrice), SUM(oi.quantity) FROM OrderItem oi WHERE oi.order.orderStatus <> 'CANCELLED' GROUP BY oi.product.category.name ORDER BY SUM(oi.totalPrice) DESC")
    List<Object[]> findCategorySalesBreakdown();
}
