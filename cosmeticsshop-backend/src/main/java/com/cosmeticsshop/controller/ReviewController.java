package com.cosmeticsshop.controller;

import com.cosmeticsshop.model.Review;
import com.cosmeticsshop.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<Review> getAllReviews(@RequestParam(required = false) Long productId) {
        return productId == null ? reviewService.getAllReviews() : reviewService.getReviewsByProductId(productId);
    }

    @PostMapping
    @PreAuthorize("hasRole('INDIVIDUAL')")
    public Review addReview(@RequestBody Review review) {
        return reviewService.saveReview(review);
    }

    @GetMapping("/{id}")
    public Review getReviewById(@PathVariable Long id) {
        return reviewService.getReviewById(id);
    }

    @PatchMapping("/{id}/respond")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public Review respondToReview(@PathVariable Long id, @RequestBody String response) {
        return reviewService.respondToReview(id, response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
    }
}
