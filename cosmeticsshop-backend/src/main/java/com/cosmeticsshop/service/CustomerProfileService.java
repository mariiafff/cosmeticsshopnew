package com.cosmeticsshop.service;

import com.cosmeticsshop.exception.ResourceNotFoundException;
import com.cosmeticsshop.model.CustomerProfile;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.CustomerProfileRepository;
import com.cosmeticsshop.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;

    public CustomerProfileService(CustomerProfileRepository customerProfileRepository,
                                  UserRepository userRepository) {
        this.customerProfileRepository = customerProfileRepository;
        this.userRepository = userRepository;
    }

    public CustomerProfile getProfileByUser(User user) {
        return customerProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + user.getId()));
    }

    public CustomerProfile createProfile(CustomerProfile profile, User user) {
        if (customerProfileRepository.findByUser_Id(user.getId()).isPresent()) {
            throw new IllegalArgumentException("Profile already exists for this user.");
        }
        profile.setUser(user);
        return customerProfileRepository.save(profile);
    }

    public CustomerProfile updateProfile(Long id, CustomerProfile profile, User user) {
        CustomerProfile existing = customerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found: " + id));

        if (!existing.getUser().getId().equals(user.getId()) && !user.getRole().equals("ADMIN")) {
            throw new IllegalArgumentException("Not authorized to update this profile.");
        }

        existing.setAge(profile.getAge());
        existing.setGender(profile.getGender());
        existing.setCity(profile.getCity());
        existing.setMembershipType(profile.getMembershipType());
        existing.setTotalSpend(profile.getTotalSpend());
        existing.setItemsPurchased(profile.getItemsPurchased());
        existing.setAverageRating(profile.getAverageRating());
        existing.setSatisfactionLevel(profile.getSatisfactionLevel());
        existing.setPreferredCategory(profile.getPreferredCategory());

        return customerProfileRepository.save(existing);
    }

    public User getUserFromAuthentication(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }
}
