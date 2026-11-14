package com.quickkart.dto;

import com.quickkart.entity.enums.BannerPlacement;
import com.quickkart.entity.enums.DiscountType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponBannerReviewDtos {

    public record CouponDto(
            Long id,
            String code,
            String description,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minPurchaseAmount,
            BigDecimal maxDiscountAmount,
            LocalDateTime startDate,
            LocalDateTime expiryDate,
            Integer usageLimit,
            Integer usedCount,
            Boolean active
    ) {}

    public record CouponRequest(
            @NotBlank(message = "Coupon code is required")
            @Size(max = 60)
            String code,
            String description,
            @NotNull DiscountType discountType,
            @NotNull @DecimalMin("0.01") BigDecimal discountValue,
            @DecimalMin("0.00") BigDecimal minPurchaseAmount,
            BigDecimal maxDiscountAmount,
            LocalDateTime startDate,
            LocalDateTime expiryDate,
            Integer usageLimit,
            Boolean active
    ) {}

    public record CouponApplyRequest(
            @NotBlank String code
    ) {}

    public record BannerDto(
            Long id,
            String title,
            String subtitle,
            String description,
            String imageUrl,
            String mobileImageUrl,
            String linkUrl,
            String ctaText,
            BannerPlacement placement,
            Integer displayOrder,
            Boolean active,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {}

    public record BannerRequest(
            @NotBlank(message = "Banner title is required")
            String title,
            String subtitle,
            String description,
            @NotBlank(message = "Banner image URL is required")
            String imageUrl,
            String mobileImageUrl,
            String linkUrl,
            String ctaText,
            @NotNull BannerPlacement placement,
            Integer displayOrder,
            Boolean active,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {}

    public record ReviewDto(
            Long id,
            Long productId,
            String productName,
            Long userId,
            String userName,
            Integer rating,
            String title,
            String comment,
            Boolean verifiedPurchase,
            Boolean approved,
            LocalDateTime createdAt
    ) {}

    public record ReviewRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 160) String title,
            @NotBlank(message = "Review comment is required")
            @Size(min = 3, max = 2000)
            String comment
    ) {}

    public record StoreSettingDto(
            Long id,
            @NotBlank String storeName,
            String storeLogoUrl,
            @NotBlank @Email String contactEmail,
            String contactPhone,
            String businessAddress,
            String gstNumber,
            @NotNull @DecimalMin("0.00") BigDecimal deliveryCharge,
            @NotNull @DecimalMin("0.00") BigDecimal freeDeliveryThreshold,
            @NotNull @Min(0) Integer returnPolicyDays,
            String returnPolicyText,
            String storeAnnouncement,
            Boolean codEnabled,
            Boolean razorpayEnabled,
            String instagramUrl,
            String facebookUrl,
            String twitterUrl
    ) {}

    public record NewsletterRequest(
            @NotBlank @Email String email
    ) {}
}
