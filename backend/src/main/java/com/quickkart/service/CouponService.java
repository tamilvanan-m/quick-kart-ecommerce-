package com.quickkart.service;

import com.quickkart.dto.CouponBannerReviewDtos;
import com.quickkart.entity.Coupon;
import com.quickkart.entity.enums.DiscountType;
import com.quickkart.exception.BadRequestException;
import com.quickkart.exception.ConflictException;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.CouponRepository;
import com.quickkart.repository.CouponUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final EntityMapper mapper;
    private final AuditLogService auditLogService;

    public CouponService(
            CouponRepository couponRepository,
            CouponUsageRepository couponUsageRepository,
            EntityMapper mapper,
            AuditLogService auditLogService) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CouponBannerReviewDtos.CouponDto> getAllCoupons() {
        return couponRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toCouponDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CouponBannerReviewDtos.CouponDto> getPublicActiveCoupons() {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .filter(c -> c.getStartDate() == null || !now.isBefore(c.getStartDate()))
                .filter(c -> c.getExpiryDate() == null || !now.isAfter(c.getExpiryDate()))
                .filter(c -> c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit())
                .map(mapper::toCouponDto)
                .toList();
    }

    @Transactional
    public CouponBannerReviewDtos.CouponDto createCoupon(CouponBannerReviewDtos.CouponRequest req) {
        String code = req.code().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Coupon code '" + code + "' already exists.");
        }
        Coupon coupon = new Coupon();
        applyFields(coupon, req, code);
        Coupon saved = couponRepository.save(coupon);
        auditLogService.log("COUPON_CREATED", "COUPON", String.valueOf(saved.getId()), "Created coupon " + code);
        return mapper.toCouponDto(saved);
    }

    @Transactional
    public CouponBannerReviewDtos.CouponDto updateCoupon(Long id, CouponBannerReviewDtos.CouponRequest req) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found."));
        String code = req.code().trim().toUpperCase();
        if (!code.equalsIgnoreCase(coupon.getCode()) && couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Coupon code '" + code + "' already exists.");
        }
        applyFields(coupon, req, code);
        Coupon saved = couponRepository.save(coupon);
        auditLogService.log("COUPON_UPDATED", "COUPON", String.valueOf(saved.getId()), "Updated coupon " + code);
        return mapper.toCouponDto(saved);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found."));
        couponRepository.delete(coupon);
        auditLogService.log("COUPON_DELETED", "COUPON", String.valueOf(id), "Deleted coupon " + coupon.getCode());
    }

    public Coupon validateCouponForUserAndAmount(String code, Long userId, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException("Invalid coupon code."));

        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new BadRequestException("This coupon is currently inactive.");
        }
        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            throw new BadRequestException("This coupon is not active yet.");
        }
        if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
            throw new BadRequestException("This coupon has expired.");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BadRequestException("This coupon has reached its usage limit.");
        }
        if (subtotal.compareTo(coupon.getMinPurchaseAmount()) < 0) {
            throw new BadRequestException("Minimum order value of ₹" + coupon.getMinPurchaseAmount() + " is required for this coupon.");
        }
        if (userId != null && couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId) >= 3) {
            throw new BadRequestException("You have reached the maximum redemptions for this coupon.");
        }
        return coupon;
    }

    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private void applyFields(Coupon coupon, CouponBannerReviewDtos.CouponRequest req, String code) {
        coupon.setCode(code);
        coupon.setDescription(req.description());
        coupon.setDiscountType(req.discountType());
        coupon.setDiscountValue(req.discountValue());
        coupon.setMinPurchaseAmount(req.minPurchaseAmount() != null ? req.minPurchaseAmount() : BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(req.maxDiscountAmount());
        coupon.setStartDate(req.startDate());
        coupon.setExpiryDate(req.expiryDate());
        coupon.setUsageLimit(req.usageLimit());
        coupon.setActive(req.active() != null ? req.active() : true);
    }
}
