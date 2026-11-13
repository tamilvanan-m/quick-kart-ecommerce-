package com.quickkart.entity;

import com.quickkart.entity.enums.Gender;
import com.quickkart.entity.enums.ProductStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 220)
    private String name;

    @Column(nullable = false, unique = true, length = 240)
    private String slug;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Gender gender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "material_care", length = 500)
    private String materialCare;

    @Column(name = "original_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "discount_percentage", nullable = false)
    private Integer discountPercentage = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(name = "new_arrival", nullable = false)
    private Boolean newArrival = false;

    @Column(name = "best_seller", nullable = false)
    private Boolean bestSeller = false;

    @Column(nullable = false)
    private Boolean trending = false;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Column(name = "total_sales", nullable = false)
    private Long totalSales = 0L;

    @Column(nullable = false)
    private Boolean deleted = false;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMaterialCare() { return materialCare; }
    public void setMaterialCare(String materialCare) { this.materialCare = materialCare; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public Integer getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Integer discountPercentage) { this.discountPercentage = discountPercentage; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
    public Boolean getNewArrival() { return newArrival; }
    public void setNewArrival(Boolean newArrival) { this.newArrival = newArrival; }
    public Boolean getBestSeller() { return bestSeller; }
    public void setBestSeller(Boolean bestSeller) { this.bestSeller = bestSeller; }
    public Boolean getTrending() { return trending; }
    public void setTrending(Boolean trending) { this.trending = trending; }
    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public Long getTotalSales() { return totalSales; }
    public void setTotalSales(Long totalSales) { this.totalSales = totalSales; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }
    public List<ProductVariant> getVariants() { return variants; }
    public void setVariants(List<ProductVariant> variants) { this.variants = variants; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
