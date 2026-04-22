package com.cosmeticsshop.service;

import com.cosmeticsshop.exception.ResourceNotFoundException;
import com.cosmeticsshop.model.Review;
import com.cosmeticsshop.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    public List<Review> getReviewsByProductId(Long productId) {
        return reviewRepository.findByProduct_Id(productId);
    }

    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
    }

    public Review saveReview(Review review) {
        return reviewRepository.save(review);
    }

    public Review updateReview(Long id, Review review) {
        Review existingReview = getReviewById(id);

        existingReview.setProduct(review.getProduct());
        existingReview.setUser(review.getUser());
        existingReview.setRating(review.getRating());
        existingReview.setComment(review.getComment());
        existingReview.setTitle(review.getTitle());
        existingReview.setHelpfulVotes(review.getHelpfulVotes());
        existingReview.setTotalVotes(review.getTotalVotes());

        return reviewRepository.save(existingReview);
    }

    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }
}
