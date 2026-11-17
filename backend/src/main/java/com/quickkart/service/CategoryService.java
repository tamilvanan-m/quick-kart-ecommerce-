package com.quickkart.service;

import com.quickkart.dto.CatalogDtos;
import com.quickkart.entity.Category;
import com.quickkart.exception.ConflictException;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.CategoryRepository;
import com.quickkart.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final EntityMapper mapper;
    private final AuditLogService auditLogService;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            EntityMapper mapper,
            AuditLogService auditLogService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.CategoryDto> getPublicCategoryTree() {
        List<Category> active = categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        return buildTree(active);
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.CategoryDto> getAllFlatForAdmin() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> mapper.toCategoryDto(c, Collections.emptyList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.CategoryDto> getPublicFlat() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> mapper.toCategoryDto(c, Collections.emptyList()))
                .toList();
    }

    @Transactional
    public CatalogDtos.CategoryDto createCategory(CatalogDtos.CategoryRequest req) {
        String slug = generateUniqueSlug(req.slug() != null && !req.slug().isBlank() ? req.slug() : req.name());
        Category cat = new Category();
        cat.setName(req.name().trim());
        cat.setSlug(slug);
        cat.setDescription(req.description());
        cat.setImageUrl(req.imageUrl());
        if (req.parentId() != null) {
            Category parent = categoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found."));
            cat.setParent(parent);
        }
        cat.setGenderScope(req.genderScope() != null ? req.genderScope().trim().toUpperCase() : null);
        cat.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : 0);
        cat.setActive(req.active() != null ? req.active() : true);
        Category saved = categoryRepository.save(cat);
        auditLogService.log("CATEGORY_CREATED", "CATEGORY", String.valueOf(saved.getId()), "Created category: " + saved.getName());
        return mapper.toCategoryDto(saved, Collections.emptyList());
    }

    @Transactional
    public CatalogDtos.CategoryDto updateCategory(Long id, CatalogDtos.CategoryRequest req) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        cat.setName(req.name().trim());
        if (req.slug() != null && !req.slug().isBlank() && !req.slug().equals(cat.getSlug())) {
            cat.setSlug(generateUniqueSlug(req.slug()));
        }
        cat.setDescription(req.description());
        cat.setImageUrl(req.imageUrl());
        if (req.parentId() != null) {
            if (req.parentId().equals(id)) {
                throw new ConflictException("Category cannot be its own parent.");
            }
            Category parent = categoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found."));
            cat.setParent(parent);
        } else {
            cat.setParent(null);
        }
        cat.setGenderScope(req.genderScope() != null ? req.genderScope().trim().toUpperCase() : null);
        if (req.displayOrder() != null) cat.setDisplayOrder(req.displayOrder());
        if (req.active() != null) cat.setActive(req.active());

        Category saved = categoryRepository.save(cat);
        auditLogService.log("CATEGORY_UPDATED", "CATEGORY", String.valueOf(saved.getId()), "Updated category: " + saved.getName());
        return mapper.toCategoryDto(saved, Collections.emptyList());
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        if (productRepository.existsByCategoryIdAndDeletedFalse(id)) {
            throw new ConflictException("Cannot delete category while products are assigned to it. Reassign products or deactivate the category instead.");
        }
        if (!categoryRepository.findByParentIdAndActiveTrueOrderByDisplayOrderAsc(id).isEmpty()) {
            throw new ConflictException("Cannot delete category with active subcategories. Remove or move subcategories first.");
        }
        if (productRepository.existsByCategoryId(id)) {
            cat.setActive(false);
            categoryRepository.save(cat);
            auditLogService.log("CATEGORY_ARCHIVED", "CATEGORY", String.valueOf(id), "Archived category with historical products: " + cat.getName());
            return;
        }
        categoryRepository.delete(cat);
        auditLogService.log("CATEGORY_DELETED", "CATEGORY", String.valueOf(id), "Deleted category: " + cat.getName());
    }

    private List<CatalogDtos.CategoryDto> buildTree(List<Category> all) {
        Map<Long, List<Category>> byParent = new LinkedHashMap<>();
        List<Category> roots = new ArrayList<>();
        for (Category c : all) {
            if (c.getParent() == null) {
                roots.add(c);
            } else {
                byParent.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c);
            }
        }
        return roots.stream()
                .map(root -> {
                    List<CatalogDtos.CategoryDto> children = byParent.getOrDefault(root.getId(), Collections.emptyList())
                            .stream()
                            .map(child -> mapper.toCategoryDto(child, Collections.emptyList()))
                            .toList();
                    return mapper.toCategoryDto(root, children);
                })
                .toList();
    }

    private String generateUniqueSlug(String raw) {
        String base = raw.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "category";
        String candidate = base;
        int counter = 1;
        while (categoryRepository.existsBySlug(candidate)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }
}
