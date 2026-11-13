package com.quickkart.entity;

import com.quickkart.entity.enums.BannerPlacement;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "banners")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 200)
    private String subtitle;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url", nullable = false, length = 600)
    private String imageUrl;

    @Column(name = "mobile_image_url", length = 600)
    private String mobileImageUrl;

    @Column(name = "link_url", length = 300)
    private String linkUrl;

    @Column(name = "cta_text", length = 60)
    private String ctaText = "SHOP NOW";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BannerPlacement placement = BannerPlacement.HERO;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getMobileImageUrl() { return mobileImageUrl; }
    public void setMobileImageUrl(String mobileImageUrl) { this.mobileImageUrl = mobileImageUrl; }
    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    public String getCtaText() { return ctaText; }
    public void setCtaText(String ctaText) { this.ctaText = ctaText; }
    public BannerPlacement getPlacement() { return placement; }
    public void setPlacement(BannerPlacement placement) { this.placement = placement; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
