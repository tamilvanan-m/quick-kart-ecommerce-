package com.quickkart.dto;

import com.quickkart.entity.enums.RoleName;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AdminDtos {

    public record DashboardStatsDto(
            BigDecimal totalSales,
            BigDecimal todaySales,
            BigDecimal monthlySales,
            long totalOrders,
            long pendingOrders,
            long deliveredOrders,
            long totalProducts,
            long totalCustomers,
            long lowStockProducts,
            long outOfStockProducts,
            List<RevenuePointDto> revenueChart,
            List<OrderPaymentDtos.OrderDto> recentOrders,
            List<TopProductDto> topSellingProducts,
            List<CategorySalesDto> categorySales
    ) {}

    public record RevenuePointDto(
            String label,
            BigDecimal revenue,
            long orderCount
    ) {}

    public record TopProductDto(
            Long productId,
            String productName,
            String brandName,
            long unitsSold,
            BigDecimal revenue
    ) {}

    public record CategorySalesDto(
            String categoryName,
            BigDecimal revenue,
            long unitsSold
    ) {}

    public record InventoryItemDto(
            Long inventoryId,
            Long variantId,
            Long productId,
            String productName,
            String brandName,
            String categoryName,
            String productStatus,
            String variantSku,
            String size,
            String color,
            Integer quantity,
            Integer reservedQuantity,
            Integer availableQuantity,
            Integer lowStockThreshold,
            Boolean lowStock,
            Boolean outOfStock,
            LocalDateTime updatedAt
    ) {}

    public record UpdateInventoryRequest(
            @NotNull @Min(0) Integer quantity,
            @Min(0) Integer lowStockThreshold,
            String notes
    ) {}

    public record InventoryTransactionDto(
            Long id,
            Long variantId,
            String variantSku,
            String productName,
            Integer changeAmount,
            Integer previousQuantity,
            Integer newQuantity,
            String transactionType,
            String notes,
            LocalDateTime createdAt
    ) {}

    public record CustomerSummaryDto(
            Long id,
            String fullName,
            String email,
            String phone,
            Boolean active,
            long totalOrders,
            BigDecimal totalSpent,
            LocalDateTime createdAt
    ) {}

    public record StaffCreateRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password,
            String phone,
            @NotNull RoleName role
    ) {}

    public record StaffUpdateRequest(
            RoleName role,
            Boolean active
    ) {}

    public record AuditLogDto(
            Long id,
            Long userId,
            String userEmail,
            String userRole,
            String action,
            String entityType,
            String entityId,
            String details,
            String ipAddress,
            LocalDateTime createdAt
    ) {}
}
