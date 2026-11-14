package com.quickkart.repository;

import com.quickkart.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId, Pageable pageable);
    List<Review> findByProductIdAndApprovedTrue(Long productId);
    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Review> findTop6ByApprovedTrueAndRatingGreaterThanEqualOrderByCreatedAtDesc(Integer minRating);
}
