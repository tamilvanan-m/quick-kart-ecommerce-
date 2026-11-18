package com.quickkart.controller;

import com.quickkart.dto.AdminDtos;
import com.quickkart.dto.CatalogDtos;
import com.quickkart.dto.CouponBannerReviewDtos;
import com.quickkart.dto.OrderPaymentDtos;
import com.quickkart.entity.enums.OrderStatus;
import com.quickkart.entity.enums.PaymentStatus;
import com.quickkart.security.UserPrincipal;
import com.quickkart.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Operations", description = "Inventory, orders, returns, coupons, banners, and review moderation")
public class AdminOperationsController {

    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final CouponService couponService;
    private final BannerService bannerService;
    private final ReviewService reviewService;

    public AdminOperationsController(
            InventoryService inventoryService,
            OrderService orderService,
            CouponService couponService,
            BannerService bannerService,
            ReviewService reviewService) {
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.couponService = couponService;
        this.bannerService = bannerService;
        this.reviewService = reviewService;
    }

    // ---- INVENTORY ----
    @GetMapping("/inventory")
    @Operation(summary = "List variant-level inventory across all products")
    public ResponseEntity<List<AdminDtos.InventoryItemDto>> getInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/inventory/low-stock")
    @Operation(summary = "List low-stock and out-of-stock variants")
    public ResponseEntity<List<AdminDtos.InventoryItemDto>> getLowStock() {
        return ResponseEntity.ok(inventoryService.getLowStockInventory());
    }

    @PutMapping("/inventory/{variantId}")
    @Operation(summary = "Update variant stock quantity and automatically sync SOLD OUT/ACTIVE product status")
    public ResponseEntity<AdminDtos.InventoryItemDto> updateVariantStock(
            @PathVariable Long variantId,
            @Valid @RequestBody AdminDtos.UpdateInventoryRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(inventoryService.adjustVariantStock(variantId, req, principal.getId()));
    }

    @GetMapping("/inventory/transactions")
    @Operation(summary = "View inventory stock movement audit trail")
    public ResponseEntity<CatalogDtos.PageResponse<AdminDtos.InventoryTransactionDto>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = inventoryService.getTransactions(page, size);
        return ResponseEntity.ok(new CatalogDtos.PageResponse<>(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast()
        ));
    }

    // ---- ORDERS & RETURNS ----
    @GetMapping("/orders")
    @Operation(summary = "List and filter all customer orders")
    public ResponseEntity<CatalogDtos.PageResponse<OrderPaymentDtos.OrderDto>> listOrders(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(orderService.getAdminOrders(q, orderStatus, paymentStatus, fromDate, toDate, page, size));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "View complete order details")
    public ResponseEntity<OrderPaymentDtos.OrderDto> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getAdminOrder(id));
    }

    @PutMapping("/orders/{id}/status")
    @Operation(summary = "Update order fulfillment status or tracking details")
    public ResponseEntity<OrderPaymentDtos.OrderDto> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderPaymentDtos.UpdateOrderStatusRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.updateAdminOrderStatus(id, req, principal.getId()));
    }

    @GetMapping("/returns")
    @Operation(summary = "List all customer return requests")
    public ResponseEntity<List<OrderPaymentDtos.ReturnDto>> listReturns() {
        return ResponseEntity.ok(orderService.getAllReturns());
    }

    @PutMapping("/returns/{id}")
    @Operation(summary = "Approve, reject, or refund a return request")
    public ResponseEntity<OrderPaymentDtos.ReturnDto> processReturn(
            @PathVariable Long id,
            @Valid @RequestBody OrderPaymentDtos.ProcessReturnRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.processReturn(id, req, principal.getId()));
    }

    // ---- COUPONS ----
    @GetMapping("/coupons")
    @Operation(summary = "List all coupons")
    public ResponseEntity<List<CouponBannerReviewDtos.CouponDto>> listCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PostMapping("/coupons")
    @Operation(summary = "Create a new discount coupon")
    public ResponseEntity<CouponBannerReviewDtos.CouponDto> createCoupon(@Valid @RequestBody CouponBannerReviewDtos.CouponRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(req));
    }

    @PutMapping("/coupons/{id}")
    @Operation(summary = "Update an existing coupon")
    public ResponseEntity<CouponBannerReviewDtos.CouponDto> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponBannerReviewDtos.CouponRequest req) {
        return ResponseEntity.ok(couponService.updateCoupon(id, req));
    }

    @DeleteMapping("/coupons/{id}")
    @Operation(summary = "Delete a coupon")
    public ResponseEntity<Map<String, String>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(Map.of("message", "Coupon deleted."));
    }

    // ---- BANNERS ----
    @GetMapping("/banners")
    @Operation(summary = "List all storefront banners")
    public ResponseEntity<List<CouponBannerReviewDtos.BannerDto>> listBanners() {
        return ResponseEntity.ok(bannerService.getAllBannersAdmin());
    }

    @PostMapping("/banners")
    @Operation(summary = "Create a new storefront banner")
    public ResponseEntity<CouponBannerReviewDtos.BannerDto> createBanner(@Valid @RequestBody CouponBannerReviewDtos.BannerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.createBanner(req));
    }

    @PutMapping("/banners/{id}")
    @Operation(summary = "Update a storefront banner")
    public ResponseEntity<CouponBannerReviewDtos.BannerDto> updateBanner(
            @PathVariable Long id,
            @Valid @RequestBody CouponBannerReviewDtos.BannerRequest req) {
        return ResponseEntity.ok(bannerService.updateBanner(id, req));
    }

    @DeleteMapping("/banners/{id}")
    @Operation(summary = "Delete a storefront banner")
    public ResponseEntity<Map<String, String>> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(Map.of("message", "Banner deleted."));
    }

    // ---- REVIEWS ----
    @GetMapping("/reviews")
    @Operation(summary = "List all customer reviews")
    public ResponseEntity<CatalogDtos.PageResponse<CouponBannerReviewDtos.ReviewDto>> listReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(reviewService.getAllReviewsAdmin(page, size));
    }

    @PutMapping("/reviews/{id}/approval")
    @Operation(summary = "Approve or hide a customer review")
    public ResponseEntity<CouponBannerReviewDtos.ReviewDto> toggleReviewApproval(
            @PathVariable Long id,
            @RequestParam boolean approved) {
        return ResponseEntity.ok(reviewService.toggleReviewApproval(id, approved));
    }

    @DeleteMapping("/reviews/{id}")
    @Operation(summary = "Delete a review")
    public ResponseEntity<Map<String, String>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(Map.of("message", "Review deleted."));
    }
}
