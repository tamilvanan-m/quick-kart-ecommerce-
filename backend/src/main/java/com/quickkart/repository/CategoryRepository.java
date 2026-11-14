package com.quickkart.repository;

import com.quickkart.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Category> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    List<Category> findAllByOrderByDisplayOrderAscNameAsc();
    List<Category> findByParentIdAndActiveTrueOrderByDisplayOrderAsc(Long parentId);
    List<Category> findByParentIsNullAndActiveTrueOrderByDisplayOrderAsc();
}
