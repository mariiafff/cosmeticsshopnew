package com.cosmeticsshop.repository;

import com.cosmeticsshop.model.User;
import com.cosmeticsshop.dto.UserSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Page<UserSummaryDto> findByOrderByEmailAsc(Pageable pageable);
}
