package com.cosmeticsshop.repository;

import com.cosmeticsshop.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct_Id(Long productId);

    List<Review> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<Review> findFirstByUser_IdAndProduct_IdOrderByCreatedAtDesc(Long userId, Long productId);
}
