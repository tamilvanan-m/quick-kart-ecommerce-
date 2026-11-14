package com.quickkart.repository;

import com.quickkart.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findBySlug(String slug);
    Optional<Brand> findByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
    List<Brand> findByActiveTrueOrderByNameAsc();
    List<Brand> findAllByOrderByNameAsc();
}
