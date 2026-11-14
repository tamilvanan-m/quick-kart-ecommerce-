package com.quickkart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class CartWishlistDtos {

    public record AddCartItemRequest(
            @NotNull(message = "Product ID is required")
            Long productId,

            @NotNull(message = "Variant ID is required")
            Long variantId,

            @NotNull(message = "Quantity is required")
            @Min(1)
            Integer quantity
    ) {}

    public record UpdateCartItemRequest(
            @Min(1)
            Integer quantity,
            Long newVariantId
    ) {}

    public record CartItemDto(
            Long id,
            Long productId,
            String productName,
            String productSlug,
            String brandName,
            String imageUrl,
            Long variantId,
            String size,
            String color,
            String colorHex,
            BigDecimal unitPrice,
            BigDecimal originalPrice,
            Integer quantity,
            BigDecimal lineTotal,
            Integer availableStock,
            Boolean soldOut
    ) {}

    public record CartDto(
            Long id,
            List<CartItemDto> items,
            Integer totalItems,
            BigDecimal mrpTotal,
            BigDecimal subtotal,
            BigDecimal productSavings,
            String couponCode,
            BigDecimal couponDiscount,
            BigDecimal deliveryCharge,
            BigDecimal freeDeliveryThreshold,
            BigDecimal finalAmount
    ) {}

    public record WishlistItemDto(
            Long id,
            Long productId,
            CatalogDtos.ProductDto product
    ) {}

    public record WishlistDto(
            Long id,
            List<WishlistItemDto> items
    ) {}
}
