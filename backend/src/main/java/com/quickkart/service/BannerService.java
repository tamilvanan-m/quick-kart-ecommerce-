package com.quickkart.service;

import com.quickkart.dto.CouponBannerReviewDtos;
import com.quickkart.entity.Banner;
import com.quickkart.entity.enums.BannerPlacement;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.BannerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BannerService {

    private final BannerRepository bannerRepository;
    private final EntityMapper mapper;
    private final AuditLogService auditLogService;

    public BannerService(BannerRepository bannerRepository, EntityMapper mapper, AuditLogService auditLogService) {
        this.bannerRepository = bannerRepository;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CouponBannerReviewDtos.BannerDto> getActiveBanners(BannerPlacement placement) {
        LocalDateTime now = LocalDateTime.now();
        List<Banner> list = placement != null
                ? bannerRepository.findByActiveTrueAndPlacementOrderByDisplayOrderAsc(placement)
                : bannerRepository.findByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();

        return list.stream()
                .filter(b -> b.getStartDate() == null || !now.isBefore(b.getStartDate()))
                .filter(b -> b.getEndDate() == null || !now.isAfter(b.getEndDate()))
                .map(mapper::toBannerDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CouponBannerReviewDtos.BannerDto> getAllBannersAdmin() {
        return bannerRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc().stream()
                .map(mapper::toBannerDto)
                .toList();
    }

    @Transactional
    public CouponBannerReviewDtos.BannerDto createBanner(CouponBannerReviewDtos.BannerRequest req) {
        Banner b = new Banner();
        applyFields(b, req);
        Banner saved = bannerRepository.save(b);
        auditLogService.log("BANNER_CREATED", "BANNER", String.valueOf(saved.getId()), "Created banner: " + saved.getTitle());
        return mapper.toBannerDto(saved);
    }

    @Transactional
    public CouponBannerReviewDtos.BannerDto updateBanner(Long id, CouponBannerReviewDtos.BannerRequest req) {
        Banner b = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found."));
        applyFields(b, req);
        Banner saved = bannerRepository.save(b);
        auditLogService.log("BANNER_UPDATED", "BANNER", String.valueOf(saved.getId()), "Updated banner: " + saved.getTitle());
        return mapper.toBannerDto(saved);
    }

    @Transactional
    public void deleteBanner(Long id) {
        Banner b = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found."));
        bannerRepository.delete(b);
        auditLogService.log("BANNER_DELETED", "BANNER", String.valueOf(id), "Deleted banner: " + b.getTitle());
    }

    private void applyFields(Banner b, CouponBannerReviewDtos.BannerRequest req) {
        b.setTitle(req.title().trim());
        b.setSubtitle(req.subtitle());
        b.setDescription(req.description());
        b.setImageUrl(req.imageUrl().trim());
        b.setMobileImageUrl(req.mobileImageUrl());
        b.setLinkUrl(req.linkUrl());
        b.setCtaText(req.ctaText() != null && !req.ctaText().isBlank() ? req.ctaText().trim() : "SHOP NOW");
        b.setPlacement(req.placement());
        b.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : 0);
        b.setActive(req.active() != null ? req.active() : true);
        b.setStartDate(req.startDate());
        b.setEndDate(req.endDate());
    }
}
