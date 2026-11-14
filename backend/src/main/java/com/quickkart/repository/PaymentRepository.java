package com.quickkart.repository;

import com.quickkart.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);
}
