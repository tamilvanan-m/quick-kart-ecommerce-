package com.quickkart.repository;

import com.quickkart.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Optional<ReturnRequest> findByOrderId(Long orderId);
    List<ReturnRequest> findAllByOrderByCreatedAtDesc();
}
