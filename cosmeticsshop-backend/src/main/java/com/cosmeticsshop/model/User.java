package com.cosmeticsshop.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email", unique = true),
                @Index(name = "idx_user_role", columnList = "role"),
                @Index(name = "idx_user_store", columnList = "storeId")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60)
    private String firstName;

    @Column(length = 60)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String role = "INDIVIDUAL";

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    private Long storeId;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private CustomerProfile customerProfile;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String membershipType;

    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public User(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // getters and setters
    public Long getId() { return id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public CustomerProfile getCustomerProfile() {
        return customerProfile;
    }

    public void setCustomerProfile(CustomerProfile customerProfile) {
        this.customerProfile = customerProfile;
    }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { this.membershipType = membershipType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
