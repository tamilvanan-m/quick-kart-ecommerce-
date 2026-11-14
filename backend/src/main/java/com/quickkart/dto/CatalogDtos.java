package com.quickkart.dto;

import com.quickkart.entity.enums.Gender;
import com.quickkart.entity.enums.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CatalogDtos {

    public record CategoryDto(
            Long id,
            String name,
            String slug,
            String description,
            String imageUrl,
            Long parentId,
            String parentName,
            String genderScope,
            Integer displayOrder,
            Boolean active,
            List<CategoryDto> children
    ) {}

    public record CategoryRequest(
            @NotBlank(message = "Category name is required")
            @Size(max = 100)
            String name,
            String slug,
            String description,
            String imageUrl,
            Long parentId,
            String genderScope,
            Integer displayOrder,
            Boolean active
    ) {}

    public record BrandDto(
            Long id,
            String name,
            String slug,
            String description,
            String logoUrl,
            Boolean active
    ) {}

    public record BrandRequest(
            @NotBlank(message = "Brand name is required")
            @Size(max = 120)
            String name,
            String slug,
            String description,
            String logoUrl,
            Boolean active
    ) {}

    public record ProductImageDto(
            Long id,
            String imageUrl,
            String publicId,
            Boolean isPrimary,
            Integer displayOrder,
            String altText
    ) {}

    public record ProductImageRequest(
            @NotBlank(message = "Image URL is required")
            String imageUrl,
            String publicId,
            Boolean isPrimary,
            Integer displayOrder,
            String altText
    ) {}

    public record ProductVariantDto(
            Long id,
            String sku,
            String size,
            String color,
            String colorHex,
            BigDecimal priceOverride,
            Boolean active,
            Integer stockQuantity,
            Integer reservedQuantity,
            Integer availableQuantity,
            Integer lowStockThreshold
    ) {}

    public record ProductVariantRequest(
            Long id,
            String sku,
            @NotBlank(message = "Size is required")
            String size,
            @NotBlank(message = "Color is required")
            String color,
            String colorHex,
            BigDecimal priceOverride,
            Boolean active,
            @NotNull(message = "Stock quantity is required")
            @Min(0)
            Integer stockQuantity,
            Integer lowStockThreshold
    ) {}

    public record ProductDto(
            Long id,
            String name,
            String slug,
            String sku,
            Long brandId,
            String brandName,
            String brandSlug,
            Long categoryId,
            String categoryName,
            String categorySlug,
            Gender gender,
            String description,
            String materialCare,
            BigDecimal originalPrice,
            BigDecimal sellingPrice,
            Integer discountPercentage,
            ProductStatus status,
            Boolean featured,
            Boolean newArrival,
            Boolean bestSeller,
            Boolean trending,
            BigDecimal ratingAvg,
            Integer reviewCount,
            Long totalSales,
            Integer totalAvailableStock,
            Boolean soldOut,
            String primaryImageUrl,
            List<ProductImageDto> images,
            List<ProductVariantDto> variants,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ProductRequest(
            @NotBlank(message = "Product name is required")
            @Size(max = 220)
            String name,

            String sku,

            @NotNull(message = "Brand is required")
            Long brandId,

            @NotNull(message = "Category is required")
            Long categoryId,

            @NotNull(message = "Gender is required")
            Gender gender,

            @NotBlank(message = "Description is required")
            String description,

            String materialCare,

            @NotNull(message = "Original price is required")
            @DecimalMin("0.01")
            BigDecimal originalPrice,

            @NotNull(message = "Selling price is required")
            @DecimalMin("0.01")
            BigDecimal sellingPrice,

            ProductStatus status,
            Boolean featured,
            Boolean newArrival,
            Boolean bestSeller,
            Boolean trending,

            @Valid
            List<ProductImageRequest> images,

            @Valid
            @NotEmpty(message = "At least one size/color variant is required")
            List<ProductVariantRequest> variants
    ) {}

    public record ProductSuggestionDto(
            Long id,
            String name,
            String slug,
            String brandName,
            String categoryName,
            String imageUrl,
            BigDecimal sellingPrice
    ) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {}
}
