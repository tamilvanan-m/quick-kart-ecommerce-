package com.quickkart.repository;

import com.quickkart.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCouponIdAndUserId(Long couponId, Long userId);
}
