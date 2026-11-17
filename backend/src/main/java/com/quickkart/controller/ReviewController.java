package com.quickkart.controller;

import com.quickkart.dto.CatalogDtos;
import com.quickkart.dto.CouponBannerReviewDtos;
import com.quickkart.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Product Reviews", description = "Verified purchaser ratings and reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get approved reviews for a product")
    public ResponseEntity<CatalogDtos.PageResponse<CouponBannerReviewDtos.ReviewDto>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId, page, size));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured customer reviews for homepage")
    public ResponseEntity<List<CouponBannerReviewDtos.ReviewDto>> getFeaturedReviews() {
        return ResponseEntity.ok(reviewService.getFeaturedHomepageReviews());
    }

    @PostMapping("/product/{productId}")
    @Operation(summary = "Submit a verified purchaser review")
    public ResponseEntity<CouponBannerReviewDtos.ReviewDto> submitReview(
            @PathVariable Long productId,
            @Valid @RequestBody CouponBannerReviewDtos.ReviewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.submitReview(productId, req));
    }
}
