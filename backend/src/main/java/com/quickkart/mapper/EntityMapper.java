package com.quickkart.mapper;

import com.quickkart.dto.AddressDtos;
import com.quickkart.dto.AdminDtos;
import com.quickkart.dto.AuthDtos;
import com.quickkart.dto.CatalogDtos;
import com.quickkart.dto.CouponBannerReviewDtos;
import com.quickkart.dto.OrderPaymentDtos;
import com.quickkart.entity.*;
import com.quickkart.entity.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class EntityMapper {

    public AuthDtos.UserProfileDto toUserProfileDto(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .sorted()
                .toList();
        return new AuthDtos.UserProfileDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getActive(),
                roles,
                user.getCreatedAt()
        );
    }

    public AddressDtos.AddressDto toAddressDto(Address address) {
        return new AddressDtos.AddressDto(
                address.getId(),
                address.getFullName(),
                address.getPhone(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getAddressType(),
                address.getIsDefault()
        );
    }

    public CatalogDtos.CategoryDto toCategoryDto(Category category, List<CatalogDtos.CategoryDto> children) {
        return new CatalogDtos.CategoryDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getParent() != null ? category.getParent().getName() : null,
                category.getGenderScope(),
                category.getDisplayOrder(),
                category.getActive(),
                children != null ? children : Collections.emptyList()
        );
    }

    public CatalogDtos.BrandDto toBrandDto(Brand brand) {
        return new CatalogDtos.BrandDto(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.getDescription(),
                brand.getLogoUrl(),
                brand.getActive()
        );
    }

    public CatalogDtos.ProductImageDto toProductImageDto(ProductImage img) {
        return new CatalogDtos.ProductImageDto(
                img.getId(),
                img.getImageUrl(),
                img.getPublicId(),
                img.getIsPrimary(),
                img.getDisplayOrder(),
                img.getAltText()
        );
    }

    public CatalogDtos.ProductVariantDto toProductVariantDto(ProductVariant variant) {
        Inventory inv = variant.getInventory();
        int qty = inv != null ? inv.getQuantity() : 0;
        int reserved = inv != null ? inv.getReservedQuantity() : 0;
        int available = inv != null ? inv.getAvailableQuantity() : 0;
        int threshold = inv != null ? inv.getLowStockThreshold() : 5;
        return new CatalogDtos.ProductVariantDto(
                variant.getId(),
                variant.getSku(),
                variant.getSize(),
                variant.getColor(),
                variant.getColorHex(),
                variant.getPriceOverride(),
                variant.getActive(),
                qty,
                reserved,
                available,
                threshold
        );
    }

    public CatalogDtos.ProductDto toProductDto(Product product) {
        List<CatalogDtos.ProductImageDto> imageDtos = product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getDisplayOrder))
                .map(this::toProductImageDto)
                .toList();

        String primaryImageUrl = imageDtos.stream()
                .filter(img -> Boolean.TRUE.equals(img.isPrimary()))
                .map(CatalogDtos.ProductImageDto::imageUrl)
                .findFirst()
                .orElse(imageDtos.isEmpty() ? null : imageDtos.get(0).imageUrl());

        List<CatalogDtos.ProductVariantDto> variantDtos = product.getVariants().stream()
                .map(this::toProductVariantDto)
                .toList();

        int totalAvailableStock = variantDtos.stream()
                .filter(v -> Boolean.TRUE.equals(v.active()))
                .mapToInt(CatalogDtos.ProductVariantDto::availableQuantity)
                .sum();

        boolean soldOut = totalAvailableStock <= 0 || product.getStatus() == ProductStatus.OUT_OF_STOCK;

        return new CatalogDtos.ProductDto(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getBrand().getSlug(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getGender(),
                product.getDescription(),
                product.getMaterialCare(),
                product.getOriginalPrice(),
                product.getSellingPrice(),
                product.getDiscountPercentage(),
                product.getStatus(),
                product.getFeatured(),
                product.getNewArrival(),
                product.getBestSeller(),
                product.getTrending(),
                product.getRatingAvg(),
                product.getReviewCount(),
                product.getTotalSales(),
                totalAvailableStock,
                soldOut,
                primaryImageUrl,
                imageDtos,
                variantDtos,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public <T> CatalogDtos.PageResponse<T> toPageResponse(Page<T> page) {
        return new CatalogDtos.PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public OrderPaymentDtos.OrderItemDto toOrderItemDto(OrderItem item) {
        return new OrderPaymentDtos.OrderItemDto(
                item.getId(),
                item.getProduct().getId(),
                item.getVariant().getId(),
                item.getProductName(),
                item.getBrandName(),
                item.getSize(),
                item.getColor(),
                item.getSku(),
                item.getImageUrl(),
                item.getUnitPrice(),
                item.getOriginalPrice(),
                item.getQuantity(),
                item.getTotalPrice()
        );
    }

    public OrderPaymentDtos.OrderDto toOrderDto(Order order, OrderPaymentDtos.PaymentInitiateDto paymentDetails, OrderPaymentDtos.ReturnDto returnDto) {
        List<OrderPaymentDtos.OrderItemDto> items = order.getItems().stream()
                .map(this::toOrderItemDto)
                .toList();

        return new OrderPaymentDtos.OrderDto(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getId(),
                order.getUser().getFullName(),
                order.getUser().getEmail(),
                order.getShippingFullName(),
                order.getShippingPhone(),
                order.getShippingAddressLine1(),
                order.getShippingAddressLine2(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingPostalCode(),
                order.getShippingCountry(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getDeliveryCharge(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getCouponCode(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getOrderStatus(),
                order.getEstimatedDeliveryDate(),
                order.getTrackingNumber(),
                order.getNotes(),
                order.getCancellationReason(),
                items,
                paymentDetails,
                returnDto,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public CouponBannerReviewDtos.CouponDto toCouponDto(Coupon c) {
        return new CouponBannerReviewDtos.CouponDto(
                c.getId(),
                c.getCode(),
                c.getDescription(),
                c.getDiscountType(),
                c.getDiscountValue(),
                c.getMinPurchaseAmount(),
                c.getMaxDiscountAmount(),
                c.getStartDate(),
                c.getExpiryDate(),
                c.getUsageLimit(),
                c.getUsedCount(),
                c.getActive()
        );
    }

    public CouponBannerReviewDtos.BannerDto toBannerDto(Banner b) {
        return new CouponBannerReviewDtos.BannerDto(
                b.getId(),
                b.getTitle(),
                b.getSubtitle(),
                b.getDescription(),
                b.getImageUrl(),
                b.getMobileImageUrl(),
                b.getLinkUrl(),
                b.getCtaText(),
                b.getPlacement(),
                b.getDisplayOrder(),
                b.getActive(),
                b.getStartDate(),
                b.getEndDate()
        );
    }

    public CouponBannerReviewDtos.ReviewDto toReviewDto(Review r) {
        return new CouponBannerReviewDtos.ReviewDto(
                r.getId(),
                r.getProduct().getId(),
                r.getProduct().getName(),
                r.getUser().getId(),
                r.getUser().getFullName(),
                r.getRating(),
                r.getTitle(),
                r.getComment(),
                r.getVerifiedPurchase(),
                r.getApproved(),
                r.getCreatedAt()
        );
    }

    public OrderPaymentDtos.ReturnDto toReturnDto(ReturnRequest r) {
        return new OrderPaymentDtos.ReturnDto(
                r.getId(),
                r.getOrder().getId(),
                r.getOrder().getOrderNumber(),
                r.getUser().getId(),
                r.getUser().getFullName(),
                r.getReason(),
                r.getComments(),
                r.getStatus(),
                r.getRefundAmount(),
                r.getAdminNotes(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    public CouponBannerReviewDtos.StoreSettingDto toStoreSettingDto(StoreSetting s) {
        return new CouponBannerReviewDtos.StoreSettingDto(
                s.getId(),
                s.getStoreName(),
                s.getStoreLogoUrl(),
                s.getContactEmail(),
                s.getContactPhone(),
                s.getBusinessAddress(),
                s.getGstNumber(),
                s.getDeliveryCharge(),
                s.getFreeDeliveryThreshold(),
                s.getReturnPolicyDays(),
                s.getReturnPolicyText(),
                s.getStoreAnnouncement(),
                s.getCodEnabled(),
                s.getRazorpayEnabled(),
                s.getInstagramUrl(),
                s.getFacebookUrl(),
                s.getTwitterUrl()
        );
    }

    public AdminDtos.AuditLogDto toAuditLogDto(AuditLog log) {
        return new AdminDtos.AuditLogDto(
                log.getId(),
                log.getUserId(),
                log.getUserEmail(),
                log.getUserRole(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
