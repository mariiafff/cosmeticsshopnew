package com.cosmeticsshop.controller;

import com.cosmeticsshop.model.CustomerProfile;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.service.CustomerProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer-profiles")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    public CustomerProfileController(CustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/me")
    public CustomerProfile getMyProfile(Authentication authentication) {
        User user = customerProfileService.getUserFromAuthentication(authentication);
        return customerProfileService.getProfileByUser(user);
    }

    @PostMapping
    public CustomerProfile createProfile(@RequestBody CustomerProfile profile, Authentication authentication) {
        User user = customerProfileService.getUserFromAuthentication(authentication);
        return customerProfileService.createProfile(profile, user);
    }

    @PutMapping("/{id}")
    public CustomerProfile updateProfile(@PathVariable Long id, @RequestBody CustomerProfile profile, Authentication authentication) {
        User user = customerProfileService.getUserFromAuthentication(authentication);
        return customerProfileService.updateProfile(id, profile, user);
    }
}
