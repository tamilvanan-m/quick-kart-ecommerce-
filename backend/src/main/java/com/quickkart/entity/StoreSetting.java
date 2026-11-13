package com.quickkart.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "store_settings")
public class StoreSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_name", nullable = false, length = 120)
    private String storeName = "Quick kart";

    @Column(name = "store_logo_url", length = 500)
    private String storeLogoUrl;

    @Column(name = "contact_email", nullable = false, length = 160)
    private String contactEmail = "support@quickkart.com";

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "business_address", length = 500)
    private String businessAddress;

    @Column(name = "gst_number", length = 40)
    private String gstNumber;

    @Column(name = "delivery_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryCharge = new BigDecimal("79.00");

    @Column(name = "free_delivery_threshold", nullable = false, precision = 12, scale = 2)
    private BigDecimal freeDeliveryThreshold = new BigDecimal("999.00");

    @Column(name = "return_policy_days", nullable = false)
    private Integer returnPolicyDays = 14;

    @Column(name = "return_policy_text", columnDefinition = "TEXT")
    private String returnPolicyText;

    @Column(name = "store_announcement", length = 300)
    private String storeAnnouncement;

    @Column(name = "cod_enabled", nullable = false)
    private Boolean codEnabled = true;

    @Column(name = "razorpay_enabled", nullable = false)
    private Boolean razorpayEnabled = true;

    @Column(name = "instagram_url", length = 300)
    private String instagramUrl;

    @Column(name = "facebook_url", length = 300)
    private String facebookUrl;

    @Column(name = "twitter_url", length = 300)
    private String twitterUrl;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getStoreLogoUrl() { return storeLogoUrl; }
    public void setStoreLogoUrl(String storeLogoUrl) { this.storeLogoUrl = storeLogoUrl; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }
    public BigDecimal getDeliveryCharge() { return deliveryCharge; }
    public void setDeliveryCharge(BigDecimal deliveryCharge) { this.deliveryCharge = deliveryCharge; }
    public BigDecimal getFreeDeliveryThreshold() { return freeDeliveryThreshold; }
    public void setFreeDeliveryThreshold(BigDecimal freeDeliveryThreshold) { this.freeDeliveryThreshold = freeDeliveryThreshold; }
    public Integer getReturnPolicyDays() { return returnPolicyDays; }
    public void setReturnPolicyDays(Integer returnPolicyDays) { this.returnPolicyDays = returnPolicyDays; }
    public String getReturnPolicyText() { return returnPolicyText; }
    public void setReturnPolicyText(String returnPolicyText) { this.returnPolicyText = returnPolicyText; }
    public String getStoreAnnouncement() { return storeAnnouncement; }
    public void setStoreAnnouncement(String storeAnnouncement) { this.storeAnnouncement = storeAnnouncement; }
    public Boolean getCodEnabled() { return codEnabled; }
    public void setCodEnabled(Boolean codEnabled) { this.codEnabled = codEnabled; }
    public Boolean getRazorpayEnabled() { return razorpayEnabled; }
    public void setRazorpayEnabled(Boolean razorpayEnabled) { this.razorpayEnabled = razorpayEnabled; }
    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }
    public String getFacebookUrl() { return facebookUrl; }
    public void setFacebookUrl(String facebookUrl) { this.facebookUrl = facebookUrl; }
    public String getTwitterUrl() { return twitterUrl; }
    public void setTwitterUrl(String twitterUrl) { this.twitterUrl = twitterUrl; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
