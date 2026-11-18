package com.quickkart.controller;

import com.quickkart.dto.AdminDtos;
import com.quickkart.dto.AuthDtos;
import com.quickkart.dto.CatalogDtos;
import com.quickkart.dto.CouponBannerReviewDtos;
import com.quickkart.security.UserPrincipal;
import com.quickkart.service.AdminDashboardService;
import com.quickkart.service.AdminUserService;
import com.quickkart.service.AuditLogService;
import com.quickkart.service.StoreSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Analytics, Staff & Settings", description = "Dashboard analytics, CSV reports, customers, staff management, store settings, and audit logs")
public class AdminManagementController {

    private final AdminDashboardService dashboardService;
    private final AdminUserService adminUserService;
    private final StoreSettingService storeSettingService;
    private final AuditLogService auditLogService;

    public AdminManagementController(
            AdminDashboardService dashboardService,
            AdminUserService adminUserService,
            StoreSettingService storeSettingService,
            AuditLogService auditLogService) {
        this.dashboardService = dashboardService;
        this.adminUserService = adminUserService;
        this.storeSettingService = storeSettingService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get live dashboard metrics, sales chart, recent orders, and top products")
    public ResponseEntity<AdminDtos.DashboardStatsDto> getDashboardStats(@RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(dashboardService.getDashboardStats(days));
    }

    @GetMapping("/dashboard/revenue")
    @Operation(summary = "Get daily revenue time series")
    public ResponseEntity<List<AdminDtos.RevenuePointDto>> getRevenue(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(dashboardService.getRevenueSeries(days));
    }

    @GetMapping("/reports/orders/export")
    @Operation(summary = "Export orders report as CSV")
    public ResponseEntity<byte[]> exportOrdersCsv() {
        String csv = dashboardService.exportOrdersCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quickkart-orders-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    // ---- CUSTOMERS ----
    @GetMapping("/customers")
    @Operation(summary = "List registered customers with order stats")
    public ResponseEntity<CatalogDtos.PageResponse<AdminDtos.CustomerSummaryDto>> listCustomers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(adminUserService.getCustomers(q, page, size));
    }

    @PutMapping("/customers/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Activate or deactivate a customer account")
    public ResponseEntity<Map<String, String>> toggleCustomerStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        adminUserService.toggleCustomerStatus(id, active);
        return ResponseEntity.ok(Map.of("message", "Customer status updated."));
    }

    // ---- STAFF MANAGEMENT (OWNER ONLY) ----
    @GetMapping("/staff")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "List all store staff and admin accounts (OWNER only)")
    public ResponseEntity<List<AuthDtos.UserProfileDto>> listStaff() {
        return ResponseEntity.ok(adminUserService.getStaffList());
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Create a new ADMIN or STAFF account (OWNER only)")
    public ResponseEntity<AuthDtos.UserProfileDto> createStaff(@Valid @RequestBody AdminDtos.StaffCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createStaff(req));
    }

    @PutMapping("/staff/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Update staff role or active status (OWNER only)")
    public ResponseEntity<AuthDtos.UserProfileDto> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.StaffUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adminUserService.updateStaff(id, req, principal.getId()));
    }

    // ---- STORE SETTINGS ----
    @GetMapping("/settings")
    @Operation(summary = "Get store settings")
    public ResponseEntity<CouponBannerReviewDtos.StoreSettingDto> getSettings() {
        return ResponseEntity.ok(storeSettingService.getSettings());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Update store settings (OWNER/ADMIN)")
    public ResponseEntity<CouponBannerReviewDtos.StoreSettingDto> updateSettings(
            @Valid @RequestBody CouponBannerReviewDtos.StoreSettingDto req) {
        return ResponseEntity.ok(storeSettingService.updateSettings(req));
    }

    // ---- AUDIT LOGS ----
    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "View administrative audit logs")
    public ResponseEntity<CatalogDtos.PageResponse<AdminDtos.AuditLogDto>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        var result = auditLogService.getLogs(page, size);
        return ResponseEntity.ok(new CatalogDtos.PageResponse<>(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast()
        ));
    }
}
