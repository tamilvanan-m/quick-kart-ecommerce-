package com.quickkart.service;

import com.quickkart.dto.CouponBannerReviewDtos;
import com.quickkart.entity.StoreSetting;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.StoreSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class StoreSettingService {

    private final StoreSettingRepository storeSettingRepository;
    private final EntityMapper mapper;
    private final AuditLogService auditLogService;

    public StoreSettingService(
            StoreSettingRepository storeSettingRepository,
            EntityMapper mapper,
            AuditLogService auditLogService) {
        this.storeSettingRepository = storeSettingRepository;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public StoreSetting getOrCreateEntity() {
        return storeSettingRepository.findAll().stream().findFirst().orElseGet(() -> {
            StoreSetting s = new StoreSetting();
            s.setStoreName("Quick kart");
            s.setContactEmail("support@quickkart.com");
            s.setContactPhone("+91 98765 43210");
            s.setBusinessAddress("42 Commercial Street, Bengaluru, Karnataka 560001");
            s.setGstNumber("29ABCDE1234F1Z5");
            s.setDeliveryCharge(new BigDecimal("79.00"));
            s.setFreeDeliveryThreshold(new BigDecimal("999.00"));
            s.setReturnPolicyDays(14);
            s.setReturnPolicyText("Easy 14-day returns and exchanges on unworn items with original tags intact.");
            s.setStoreAnnouncement("COMPLIMENTARY EXPRESS DELIVERY ON ORDERS ABOVE ₹999 • USE CODE WELCOME10 FOR 10% OFF");
            s.setCodEnabled(true);
            s.setRazorpayEnabled(true);
            return storeSettingRepository.save(s);
        });
    }

    @Transactional(readOnly = true)
    public CouponBannerReviewDtos.StoreSettingDto getSettings() {
        return mapper.toStoreSettingDto(getOrCreateEntity());
    }

    @Transactional
    public CouponBannerReviewDtos.StoreSettingDto updateSettings(CouponBannerReviewDtos.StoreSettingDto req) {
        StoreSetting s = getOrCreateEntity();
        s.setStoreName(req.storeName().trim());
        s.setStoreLogoUrl(req.storeLogoUrl());
        s.setContactEmail(req.contactEmail().trim());
        s.setContactPhone(req.contactPhone());
        s.setBusinessAddress(req.businessAddress());
        s.setGstNumber(req.gstNumber());
        s.setDeliveryCharge(req.deliveryCharge());
        s.setFreeDeliveryThreshold(req.freeDeliveryThreshold());
        s.setReturnPolicyDays(req.returnPolicyDays());
        s.setReturnPolicyText(req.returnPolicyText());
        s.setStoreAnnouncement(req.storeAnnouncement());
        s.setCodEnabled(req.codEnabled() != null ? req.codEnabled() : true);
        s.setRazorpayEnabled(req.razorpayEnabled() != null ? req.razorpayEnabled() : true);
        s.setInstagramUrl(req.instagramUrl());
        s.setFacebookUrl(req.facebookUrl());
        s.setTwitterUrl(req.twitterUrl());
        StoreSetting saved = storeSettingRepository.save(s);
        auditLogService.log("SETTINGS_UPDATED", "STORE_SETTING", String.valueOf(saved.getId()), "Store settings updated");
        return mapper.toStoreSettingDto(saved);
    }
}
