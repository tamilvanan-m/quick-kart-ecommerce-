package com.quickkart.repository;

import com.quickkart.entity.Product;
import com.quickkart.entity.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByIdAndDeletedFalse(Long id);
    Optional<Product> findBySlugAndDeletedFalse(String slug);
    boolean existsBySkuAndDeletedFalse(String sku);
    boolean existsBySlug(String slug);
    boolean existsByCategoryIdAndDeletedFalse(Long categoryId);
    boolean existsByCategoryId(Long categoryId);
    boolean existsByBrandIdAndDeletedFalse(Long brandId);
    boolean existsByBrandId(Long brandId);
    long countByDeletedFalse();
    long countByDeletedFalseAndStatus(ProductStatus status);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.brand.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.category.name) LIKE LOWER(CONCAT('%', :q, '%'))) AND p.status IN ('ACTIVE', 'OUT_OF_STOCK')")
    List<Product> searchSuggestions(@Param("q") String q);
}
