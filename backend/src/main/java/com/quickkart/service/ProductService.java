package com.quickkart.service;

import com.quickkart.dto.CatalogDtos;
import com.quickkart.entity.*;
import com.quickkart.entity.enums.Gender;
import com.quickkart.entity.enums.InventoryTransactionType;
import com.quickkart.entity.enums.ProductStatus;
import com.quickkart.exception.BadRequestException;
import com.quickkart.exception.ConflictException;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;
    private final EntityMapper mapper;
    private final AuditLogService auditLogService;

    public ProductService(
            ProductRepository productRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            OrderItemRepository orderItemRepository,
            InventoryService inventoryService,
            EntityMapper mapper,
            AuditLogService auditLogService) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryService = inventoryService;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PageResponse<CatalogDtos.ProductDto> searchPublicProducts(
            String q,
            String categorySlug,
            Long categoryId,
            String brandSlug,
            Long brandId,
            Gender gender,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String size,
            String color,
            Integer minDiscount,
            Boolean inStockOnly,
            Boolean featured,
            Boolean newArrival,
            Boolean bestSeller,
            Boolean trending,
            String sortBy,
            int page,
            int pageSize) {

        Specification<Product> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            predicates.add(root.get("status").in(ProductStatus.ACTIVE, ProductStatus.OUT_OF_STOCK));

            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("brand").get("name")), pattern),
                        cb.like(cb.lower(root.get("category").get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (categoryId != null || (categorySlug != null && !categorySlug.isBlank())) {
                Join<Product, Category> catJoin = root.join("category", JoinType.INNER);
                Join<Category, Category> parentJoin = catJoin.join("parent", JoinType.LEFT);
                if (categoryId != null) {
                    predicates.add(cb.or(
                            cb.equal(catJoin.get("id"), categoryId),
                            cb.equal(parentJoin.get("id"), categoryId)
                    ));
                }
                if (categorySlug != null && !categorySlug.isBlank()) {
                    String slug = categorySlug.trim().toLowerCase();
                    predicates.add(cb.or(
                            cb.equal(cb.lower(catJoin.get("slug")), slug),
                            cb.equal(cb.lower(parentJoin.get("slug")), slug)
                    ));
                }
            }
            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), brandId));
            }
            if (brandSlug != null && !brandSlug.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand").get("slug")), brandSlug.trim().toLowerCase()));
            }
            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sellingPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("sellingPrice"), maxPrice));
            }
            if (minDiscount != null && minDiscount > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("discountPercentage"), minDiscount));
            }
            if (Boolean.TRUE.equals(inStockOnly)) {
                predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            }
            if (Boolean.TRUE.equals(featured)) {
                predicates.add(cb.isTrue(root.get("featured")));
            }
            if (Boolean.TRUE.equals(newArrival)) {
                predicates.add(cb.isTrue(root.get("newArrival")));
            }
            if (Boolean.TRUE.equals(bestSeller)) {
                predicates.add(cb.isTrue(root.get("bestSeller")));
            }
            if (Boolean.TRUE.equals(trending)) {
                predicates.add(cb.isTrue(root.get("trending")));
            }
            if ((size != null && !size.isBlank()) || (color != null && !color.isBlank())) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                predicates.add(cb.isTrue(variantJoin.get("active")));
                if (size != null && !size.isBlank()) {
                    predicates.add(cb.equal(cb.upper(variantJoin.get("size")), size.trim().toUpperCase()));
                }
                if (color != null && !color.isBlank()) {
                    predicates.add(cb.equal(cb.lower(variantJoin.get("color")), color.trim().toLowerCase()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(48, Math.max(1, pageSize)), resolveSort(sortBy));
        Page<CatalogDtos.ProductDto> result = productRepository.findAll(spec, pageable).map(mapper::toProductDto);
        return mapper.toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.ProductDto getPublicProductByIdOrSlug(String idOrSlug) {
        Product product = resolveProductByIdOrSlug(idOrSlug);
        if (product.getStatus() == ProductStatus.INACTIVE || product.getStatus() == ProductStatus.DRAFT) {
            throw new ResourceNotFoundException("Product not found.");
        }
        return mapper.toProductDto(product);
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.ProductSuggestionDto> getSearchSuggestions(String q) {
        if (q == null || q.trim().length() < 2) {
            return Collections.emptyList();
        }
        return productRepository.searchSuggestions(q.trim()).stream()
                .limit(8)
                .map(p -> {
                    String img = p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl();
                    return new CatalogDtos.ProductSuggestionDto(
                            p.getId(),
                            p.getName(),
                            p.getSlug(),
                            p.getBrand().getName(),
                            p.getCategory().getName(),
                            img,
                            p.getSellingPrice()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PageResponse<CatalogDtos.ProductDto> getAdminProducts(String q, ProductStatus status, int page, int size) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("sku")), pattern),
                        cb.like(cb.lower(root.get("brand").get("name")), pattern)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<CatalogDtos.ProductDto> result = productRepository
                .findAll(spec, PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(mapper::toProductDto);
        return mapper.toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.ProductDto getAdminProduct(Long id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        return mapper.toProductDto(product);
    }

    @Transactional
    public CatalogDtos.ProductDto createProduct(CatalogDtos.ProductRequest req, Long actorUserId) {
        validatePrices(req.originalPrice(), req.sellingPrice());

        Brand brand = brandRepository.findById(req.brandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found."));
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        String sku = (req.sku() != null && !req.sku().isBlank()) ? req.sku().trim().toUpperCase() : generateSku(req.name());
        if (productRepository.existsBySkuAndDeletedFalse(sku)) {
            throw new ConflictException("A product with SKU '" + sku + "' already exists.");
        }

        Product product = new Product();
        product.setName(req.name().trim());
        product.setSlug(generateUniqueSlug(req.name()));
        product.setSku(sku);
        product.setBrand(brand);
        product.setCategory(category);
        product.setGender(req.gender());
        product.setDescription(req.description().trim());
        product.setMaterialCare(req.materialCare());
        product.setOriginalPrice(req.originalPrice());
        product.setSellingPrice(req.sellingPrice());
        product.setDiscountPercentage(calculateDiscountPct(req.originalPrice(), req.sellingPrice()));
        product.setStatus(req.status() != null ? req.status() : ProductStatus.ACTIVE);
        product.setFeatured(Boolean.TRUE.equals(req.featured()));
        product.setNewArrival(Boolean.TRUE.equals(req.newArrival()));
        product.setBestSeller(Boolean.TRUE.equals(req.bestSeller()));
        product.setTrending(Boolean.TRUE.equals(req.trending()));

        applyImages(product, req.images());
        applyVariantsForCreate(product, req.variants(), sku);

        Product saved = productRepository.save(product);
        inventoryService.syncProductStockStatus(saved);

        for (ProductVariant v : saved.getVariants()) {
            if (v.getInventory() != null && v.getInventory().getQuantity() > 0) {
                inventoryService.recordTransaction(
                        v, null, v.getInventory().getQuantity(), 0, v.getInventory().getQuantity(),
                        InventoryTransactionType.INITIAL_STOCK, "Initial product creation", actorUserId
                );
            }
        }

        auditLogService.log("PRODUCT_CREATED", "PRODUCT", String.valueOf(saved.getId()), "Created product: " + saved.getName());
        return mapper.toProductDto(saved);
    }

    @Transactional
    public CatalogDtos.ProductDto updateProduct(Long id, CatalogDtos.ProductRequest req, Long actorUserId) {
        validatePrices(req.originalPrice(), req.sellingPrice());

        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        Brand brand = brandRepository.findById(req.brandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found."));
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        product.setName(req.name().trim());
        if (req.sku() != null && !req.sku().isBlank() && !req.sku().trim().equalsIgnoreCase(product.getSku())) {
            String newSku = req.sku().trim().toUpperCase();
            if (productRepository.existsBySkuAndDeletedFalse(newSku)) {
                throw new ConflictException("SKU '" + newSku + "' is already used by another product.");
            }
            product.setSku(newSku);
        }
        product.setBrand(brand);
        product.setCategory(category);
        product.setGender(req.gender());
        product.setDescription(req.description().trim());
        product.setMaterialCare(req.materialCare());
        product.setOriginalPrice(req.originalPrice());
        product.setSellingPrice(req.sellingPrice());
        product.setDiscountPercentage(calculateDiscountPct(req.originalPrice(), req.sellingPrice()));

        if (req.status() != null) {
            product.setStatus(req.status());
        }
        if (req.featured() != null) product.setFeatured(req.featured());
        if (req.newArrival() != null) product.setNewArrival(req.newArrival());
        if (req.bestSeller() != null) product.setBestSeller(req.bestSeller());
        if (req.trending() != null) product.setTrending(req.trending());

        if (req.images() != null) {
            product.getImages().clear();
            applyImages(product, req.images());
        }
        if (req.variants() != null) {
            syncVariantsOnUpdate(product, req.variants(), actorUserId);
        }

        Product saved = productRepository.save(product);
        inventoryService.syncProductStockStatus(saved);
        auditLogService.log("PRODUCT_UPDATED", "PRODUCT", String.valueOf(saved.getId()), "Updated product: " + saved.getName());
        return mapper.toProductDto(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        if (orderItemRepository.existsByProductId(id)) {
            // Historical order integrity: archive via soft deletion + INACTIVE status
            product.setDeleted(true);
            product.setStatus(ProductStatus.INACTIVE);
            productRepository.save(product);
            auditLogService.log("PRODUCT_SOFT_DELETED", "PRODUCT", String.valueOf(id),
                    "Soft-deleted product with order history: " + product.getName());
        } else {
            product.setDeleted(true);
            product.setStatus(ProductStatus.INACTIVE);
            productRepository.save(product);
            auditLogService.log("PRODUCT_DELETED", "PRODUCT", String.valueOf(id), "Deleted product: " + product.getName());
        }
    }

    private Product resolveProductByIdOrSlug(String idOrSlug) {
        if (idOrSlug.chars().allMatch(Character::isDigit)) {
            return productRepository.findByIdAndDeletedFalse(Long.parseLong(idOrSlug))
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        }
        return productRepository.findBySlugAndDeletedFalse(idOrSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    private void validatePrices(BigDecimal originalPrice, BigDecimal sellingPrice) {
        if (sellingPrice.compareTo(originalPrice) > 0) {
            throw new BadRequestException("Selling price cannot exceed original price.");
        }
    }

    private int calculateDiscountPct(BigDecimal originalPrice, BigDecimal sellingPrice) {
        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0) return 0;
        BigDecimal diff = originalPrice.subtract(sellingPrice);
        if (diff.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return diff.multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private void applyImages(Product product, List<CatalogDtos.ProductImageRequest> imageReqs) {
        if (imageReqs == null || imageReqs.isEmpty()) return;
        boolean hasPrimary = imageReqs.stream().anyMatch(i -> Boolean.TRUE.equals(i.isPrimary()));
        for (int i = 0; i < imageReqs.size(); i++) {
            CatalogDtos.ProductImageRequest r = imageReqs.get(i);
            ProductImage img = new ProductImage();
            img.setProduct(product);
            img.setImageUrl(r.imageUrl().trim());
            img.setPublicId(r.publicId());
            img.setIsPrimary(hasPrimary ? Boolean.TRUE.equals(r.isPrimary()) : (i == 0));
            img.setDisplayOrder(r.displayOrder() != null ? r.displayOrder() : i);
            img.setAltText(r.altText() != null ? r.altText() : product.getName());
            product.getImages().add(img);
        }
    }

    private void applyVariantsForCreate(Product product, List<CatalogDtos.ProductVariantRequest> variantReqs, String productSku) {
        Set<String> seenKeys = new HashSet<>();
        int idx = 1;
        for (CatalogDtos.ProductVariantRequest vr : variantReqs) {
            String size = vr.size().trim().toUpperCase();
            String color = vr.color().trim();
            String key = size + "::" + color.toLowerCase();
            if (!seenKeys.add(key)) {
                throw new BadRequestException("Duplicate variant size/color combination: " + size + " / " + color);
            }
            ProductVariant v = new ProductVariant();
            v.setProduct(product);
            v.setSize(size);
            v.setColor(color);
            v.setColorHex(vr.colorHex());
            v.setPriceOverride(vr.priceOverride());
            v.setActive(vr.active() != null ? vr.active() : true);
            v.setSku(vr.sku() != null && !vr.sku().isBlank()
                    ? vr.sku().trim().toUpperCase()
                    : (productSku + "-" + size + "-" + idx++).replaceAll("[^A-Z0-9-]", ""));

            Inventory inv = new Inventory();
            inv.setVariant(v);
            inv.setQuantity(vr.stockQuantity() != null ? vr.stockQuantity() : 0);
            inv.setReservedQuantity(0);
            inv.setLowStockThreshold(vr.lowStockThreshold() != null ? vr.lowStockThreshold() : 5);
            v.setInventory(inv);
            product.getVariants().add(v);
        }
    }

    private void syncVariantsOnUpdate(Product product, List<CatalogDtos.ProductVariantRequest> variantReqs, Long actorUserId) {
        Map<Long, ProductVariant> existingById = new HashMap<>();
        for (ProductVariant ev : product.getVariants()) {
            existingById.put(ev.getId(), ev);
        }
        Set<Long> retainedIds = new HashSet<>();
        int idx = 1;

        for (CatalogDtos.ProductVariantRequest vr : variantReqs) {
            String size = vr.size().trim().toUpperCase();
            String color = vr.color().trim();
            if (vr.id() != null && existingById.containsKey(vr.id())) {
                ProductVariant ev = existingById.get(vr.id());
                ev.setSize(size);
                ev.setColor(color);
                ev.setColorHex(vr.colorHex());
                ev.setPriceOverride(vr.priceOverride());
                ev.setActive(vr.active() != null ? vr.active() : true);
                if (vr.sku() != null && !vr.sku().isBlank()) {
                    ev.setSku(vr.sku().trim().toUpperCase());
                }
                Inventory inv = ev.getInventory();
                if (inv == null) {
                    inv = new Inventory();
                    inv.setVariant(ev);
                    ev.setInventory(inv);
                }
                int prev = inv.getQuantity() != null ? inv.getQuantity() : 0;
                int next = vr.stockQuantity() != null ? vr.stockQuantity() : prev;
                if (next < (inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0)) {
                    throw new BadRequestException("Stock quantity for variant " + ev.getSku() + " cannot be below reserved units.");
                }
                inv.setQuantity(next);
                if (vr.lowStockThreshold() != null) inv.setLowStockThreshold(vr.lowStockThreshold());
                if (prev != next) {
                    inventoryService.recordTransaction(
                            ev, null, next - prev, prev, next,
                            next > prev ? InventoryTransactionType.RESTOCK : InventoryTransactionType.MANUAL_ADJUSTMENT,
                            "Product edit stock update", actorUserId
                    );
                }
                retainedIds.add(ev.getId());
            } else {
                ProductVariant nv = new ProductVariant();
                nv.setProduct(product);
                nv.setSize(size);
                nv.setColor(color);
                nv.setColorHex(vr.colorHex());
                nv.setPriceOverride(vr.priceOverride());
                nv.setActive(vr.active() != null ? vr.active() : true);
                nv.setSku(vr.sku() != null && !vr.sku().isBlank()
                        ? vr.sku().trim().toUpperCase()
                        : (product.getSku() + "-" + size + "-" + System.currentTimeMillis() % 10000 + "-" + idx++).replaceAll("[^A-Z0-9-]", ""));
                Inventory inv = new Inventory();
                inv.setVariant(nv);
                inv.setQuantity(vr.stockQuantity() != null ? vr.stockQuantity() : 0);
                inv.setReservedQuantity(0);
                inv.setLowStockThreshold(vr.lowStockThreshold() != null ? vr.lowStockThreshold() : 5);
                nv.setInventory(inv);
                product.getVariants().add(nv);
            }
        }

        // Deactivate variants removed in editor rather than hard deleting if they might exist in orders
        for (ProductVariant ev : product.getVariants()) {
            if (ev.getId() != null && !retainedIds.contains(ev.getId())) {
                ev.setActive(false);
                if (ev.getInventory() != null) {
                    ev.getInventory().setQuantity(0);
                }
            }
        }
    }

    private Sort resolveSort(String sortBy) {
        if (sortBy == null) return Sort.by(Sort.Direction.DESC, "featured", "createdAt");
        return switch (sortBy) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "sellingPrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "sellingPrice");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "popularity" -> Sort.by(Sort.Direction.DESC, "totalSales", "reviewCount");
            case "rating" -> Sort.by(Sort.Direction.DESC, "ratingAvg", "reviewCount");
            default -> Sort.by(Sort.Direction.DESC, "featured", "createdAt");
        };
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "product";
        String candidate = base;
        int counter = 1;
        while (productRepository.existsBySlug(candidate)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }

    private String generateSku(String name) {
        String prefix = name.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (prefix.length() > 5) prefix = prefix.substring(0, 5);
        if (prefix.isEmpty()) prefix = "QK";
        return prefix + "-" + (10000 + new Random().nextInt(90000));
    }
}
