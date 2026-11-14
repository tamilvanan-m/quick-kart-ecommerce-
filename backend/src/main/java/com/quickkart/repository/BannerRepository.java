package com.quickkart.repository;

import com.quickkart.entity.Banner;
import com.quickkart.entity.enums.BannerPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
    List<Banner> findByActiveTrueAndPlacementOrderByDisplayOrderAsc(BannerPlacement placement);
    List<Banner> findAllByOrderByDisplayOrderAscCreatedAtDesc();
}
