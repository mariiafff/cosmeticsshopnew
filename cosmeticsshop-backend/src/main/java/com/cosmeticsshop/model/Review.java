package com.cosmeticsshop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "star_rating")
    private int rating;

    @Column(name = "review_text", length = 1000)
    private String comment;

    @Transient
    private String title;

    private Integer helpfulVotes = 0;
    private Integer totalVotes = 0;

    @Transient
    private String sellerResponse;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Review() {}

    public Review(Product product, User user, int rating, String comment) {
        this.product = product;
        this.user = user;
        this.rating = rating;
        this.comment = comment;
    }

    // getters and setters
    public Long getId() { return id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getHelpfulVotes() { return helpfulVotes; }
    public void setHelpfulVotes(Integer helpfulVotes) { this.helpfulVotes = helpfulVotes; }

    public Integer getTotalVotes() { return totalVotes; }
    public void setTotalVotes(Integer totalVotes) { this.totalVotes = totalVotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getSellerResponse() { return sellerResponse; }
    public void setSellerResponse(String sellerResponse) { this.sellerResponse = sellerResponse; }
}
