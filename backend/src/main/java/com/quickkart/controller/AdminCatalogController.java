package com.quickkart.controller;

import com.quickkart.dto.CatalogDtos;
import com.quickkart.entity.enums.ProductStatus;
import com.quickkart.security.UserPrincipal;
import com.quickkart.service.BrandService;
import com.quickkart.service.CategoryService;
import com.quickkart.service.ProductService;
import com.quickkart.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Catalog & Media", description = "Admin endpoints for managing products, categories, brands, and image uploads")
public class AdminCatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final StorageService storageService;

    public AdminCatalogController(
            ProductService productService,
            CategoryService categoryService,
            BrandService brandService,
            StorageService storageService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.storageService = storageService;
    }

    // ---- PRODUCTS ----
    @GetMapping("/products")
    @Operation(summary = "List all products for admin management")
    public ResponseEntity<CatalogDtos.PageResponse<CatalogDtos.ProductDto>> listProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(productService.getAdminProducts(q, status, page, size));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Get product details for editing")
    public ResponseEntity<CatalogDtos.ProductDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getAdminProduct(id));
    }

    @PostMapping("/products")
    @Operation(summary = "Create a new product with images and variants")
    public ResponseEntity<CatalogDtos.ProductDto> createProduct(
            @Valid @RequestBody CatalogDtos.ProductRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(req, principal.getId()));
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Update an existing product, prices, variants, or status")
    public ResponseEntity<CatalogDtos.ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody CatalogDtos.ProductRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productService.updateProduct(id, req, principal.getId()));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Safely delete or archive a product")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully."));
    }

    // ---- CATEGORIES ----
    @GetMapping("/categories")
    @Operation(summary = "List all categories (active & inactive)")
    public ResponseEntity<List<CatalogDtos.CategoryDto>> listCategories() {
        return ResponseEntity.ok(categoryService.getAllFlatForAdmin());
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a new category")
    public ResponseEntity<CatalogDtos.CategoryDto> createCategory(@Valid @RequestBody CatalogDtos.CategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(req));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<CatalogDtos.CategoryDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CatalogDtos.CategoryRequest req) {
        return ResponseEntity.ok(categoryService.updateCategory(id, req));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Safely delete a category")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully."));
    }

    // ---- BRANDS ----
    @GetMapping("/brands")
    @Operation(summary = "List all brands")
    public ResponseEntity<List<CatalogDtos.BrandDto>> listBrands() {
        return ResponseEntity.ok(brandService.getAllBrandsAdmin());
    }

    @PostMapping("/brands")
    @Operation(summary = "Create a new brand")
    public ResponseEntity<CatalogDtos.BrandDto> createBrand(@Valid @RequestBody CatalogDtos.BrandRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.createBrand(req));
    }

    @PutMapping("/brands/{id}")
    @Operation(summary = "Update a brand")
    public ResponseEntity<CatalogDtos.BrandDto> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody CatalogDtos.BrandRequest req) {
        return ResponseEntity.ok(brandService.updateBrand(id, req));
    }

    @DeleteMapping("/brands/{id}")
    @Operation(summary = "Safely delete a brand")
    public ResponseEntity<Map<String, String>> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(Map.of("message", "Brand deleted successfully."));
    }

    // ---- IMAGE UPLOAD ----
    @PostMapping("/upload")
    @Operation(summary = "Upload image to Cloudinary (or local fallback in dev)")
    public ResponseEntity<StorageService.UploadResult> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "products") String folder) {
        return ResponseEntity.ok(storageService.uploadImage(file, folder));
    }
}
