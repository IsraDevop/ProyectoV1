package com.yala.image.repository;

import com.yala.image.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByListingIdOrderBySortOrderAsc(Long listingId);

    long countByListingId(Long listingId);
}
