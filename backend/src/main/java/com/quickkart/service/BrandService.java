package com.quickkart.service;

import com.quickkart.dto.CatalogDtos;
import com.quickkart.entity.Brand;
import com.quickkart.exception.ConflictException;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.BrandRepository;
import com.quickkart.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final EntityMapper mapper;
    private final AuditLogService auditLogService;

    public BrandService(
            BrandRepository brandRepository,
            ProductRepository productRepository,
            EntityMapper mapper,
            AuditLogService auditLogService) {
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.BrandDto> getPublicBrands() {
        return brandRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(mapper::toBrandDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.BrandDto> getAllBrandsAdmin() {
        return brandRepository.findAllByOrderByNameAsc().stream()
                .map(mapper::toBrandDto)
                .toList();
    }

    @Transactional
    public CatalogDtos.BrandDto createBrand(CatalogDtos.BrandRequest req) {
        String name = req.name().trim();
        if (brandRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("A brand with this name already exists.");
        }
        String slug = generateUniqueSlug(req.slug() != null && !req.slug().isBlank() ? req.slug() : name);
        Brand brand = new Brand();
        brand.setName(name);
        brand.setSlug(slug);
        brand.setDescription(req.description());
        brand.setLogoUrl(req.logoUrl());
        brand.setActive(req.active() != null ? req.active() : true);
        Brand saved = brandRepository.save(brand);
        auditLogService.log("BRAND_CREATED", "BRAND", String.valueOf(saved.getId()), "Created brand: " + saved.getName());
        return mapper.toBrandDto(saved);
    }

    @Transactional
    public CatalogDtos.BrandDto updateBrand(Long id, CatalogDtos.BrandRequest req) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found."));
        brand.setName(req.name().trim());
        if (req.slug() != null && !req.slug().isBlank() && !req.slug().equals(brand.getSlug())) {
            brand.setSlug(generateUniqueSlug(req.slug()));
        }
        brand.setDescription(req.description());
        brand.setLogoUrl(req.logoUrl());
        if (req.active() != null) brand.setActive(req.active());
        Brand saved = brandRepository.save(brand);
        auditLogService.log("BRAND_UPDATED", "BRAND", String.valueOf(saved.getId()), "Updated brand: " + saved.getName());
        return mapper.toBrandDto(saved);
    }

    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found."));
        if (productRepository.existsByBrandIdAndDeletedFalse(id)) {
            throw new ConflictException("Cannot delete brand while products are assigned to it. Reassign products or deactivate the brand instead.");
        }
        if (productRepository.existsByBrandId(id)) {
            brand.setActive(false);
            brandRepository.save(brand);
            auditLogService.log("BRAND_ARCHIVED", "BRAND", String.valueOf(id), "Archived brand with historical products: " + brand.getName());
            return;
        }
        brandRepository.delete(brand);
        auditLogService.log("BRAND_DELETED", "BRAND", String.valueOf(id), "Deleted brand: " + brand.getName());
    }

    private String generateUniqueSlug(String raw) {
        String base = raw.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "brand";
        String candidate = base;
        int counter = 1;
        while (brandRepository.existsBySlug(candidate)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }
}
