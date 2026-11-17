package com.quickkart.controller;

import com.quickkart.dto.CatalogDtos;
import com.quickkart.dto.CouponBannerReviewDtos;
import com.quickkart.entity.NewsletterSubscriber;
import com.quickkart.entity.enums.BannerPlacement;
import com.quickkart.entity.enums.Gender;
import com.quickkart.repository.NewsletterSubscriberRepository;
import com.quickkart.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Storefront Catalog", description = "Public endpoints for browsing products, categories, brands, banners, and store settings")
public class CatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final BannerService bannerService;
    private final StoreSettingService storeSettingService;
    private final CouponService couponService;
    private final NewsletterSubscriberRepository newsletterRepository;

    public CatalogController(
            ProductService productService,
            CategoryService categoryService,
            BrandService brandService,
            BannerService bannerService,
            StoreSettingService storeSettingService,
            CouponService couponService,
            NewsletterSubscriberRepository newsletterRepository) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.bannerService = bannerService;
        this.storeSettingService = storeSettingService;
        this.couponService = couponService;
        this.newsletterRepository = newsletterRepository;
    }

    @GetMapping("/products")
    @Operation(summary = "Browse and filter products with server-side pagination and sorting")
    public ResponseEntity<CatalogDtos.PageResponse<CatalogDtos.ProductDto>> listProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer minDiscount,
            @RequestParam(required = false) Boolean inStockOnly,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean newArrival,
            @RequestParam(required = false) Boolean bestSeller,
            @RequestParam(required = false) Boolean trending,
            @RequestParam(defaultValue = "recommended") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        return ResponseEntity.ok(productService.searchPublicProducts(
                q, category, categoryId, brand, brandId, gender, minPrice, maxPrice,
                size, color, minDiscount, inStockOnly, featured, newArrival, bestSeller, trending,
                sort, page, pageSize
        ));
    }

    @GetMapping("/products/{idOrSlug}")
    @Operation(summary = "Get detailed product info by ID or slug")
    public ResponseEntity<CatalogDtos.ProductDto> getProduct(@PathVariable String idOrSlug) {
        return ResponseEntity.ok(productService.getPublicProductByIdOrSlug(idOrSlug));
    }

    @GetMapping({"/products/search", "/products/suggestions"})
    @Operation(summary = "Get quick search autocomplete suggestions")
    public ResponseEntity<List<CatalogDtos.ProductSuggestionDto>> searchSuggestions(@RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(productService.getSearchSuggestions(q));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get active category hierarchy for storefront navigation")
    public ResponseEntity<List<CatalogDtos.CategoryDto>> getCategories() {
        return ResponseEntity.ok(categoryService.getPublicCategoryTree());
    }

    @GetMapping("/categories/flat")
    @Operation(summary = "Get flat list of active categories")
    public ResponseEntity<List<CatalogDtos.CategoryDto>> getFlatCategories() {
        return ResponseEntity.ok(categoryService.getPublicFlat());
    }

    @GetMapping("/brands")
    @Operation(summary = "Get active brands")
    public ResponseEntity<List<CatalogDtos.BrandDto>> getBrands() {
        return ResponseEntity.ok(brandService.getPublicBrands());
    }

    @GetMapping("/banners")
    @Operation(summary = "Get active storefront banners")
    public ResponseEntity<List<CouponBannerReviewDtos.BannerDto>> getBanners(@RequestParam(required = false) BannerPlacement placement) {
        return ResponseEntity.ok(bannerService.getActiveBanners(placement));
    }

    @GetMapping("/store/settings")
    @Operation(summary = "Get public store settings such as store name, delivery rules, and announcement")
    public ResponseEntity<CouponBannerReviewDtos.StoreSettingDto> getStoreSettings() {
        return ResponseEntity.ok(storeSettingService.getSettings());
    }

    @GetMapping("/coupons/public")
    @Operation(summary = "List active public promotional coupons")
    public ResponseEntity<List<CouponBannerReviewDtos.CouponDto>> getPublicCoupons() {
        return ResponseEntity.ok(couponService.getPublicActiveCoupons());
    }

    @PostMapping("/newsletter/subscribe")
    @Operation(summary = "Subscribe email to newsletter")
    public ResponseEntity<Map<String, String>> subscribeNewsletter(@Valid @RequestBody CouponBannerReviewDtos.NewsletterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (!newsletterRepository.existsByEmailIgnoreCase(email)) {
            NewsletterSubscriber sub = new NewsletterSubscriber();
            sub.setEmail(email);
            newsletterRepository.save(sub);
        }
        return ResponseEntity.ok(Map.of("message", "Thank you for subscribing to Quick kart updates."));
    }
}
