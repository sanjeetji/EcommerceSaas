package com.saas.ecommerce.repository;

import com.saas.ecommerce.model.entity.Client;
import com.saas.ecommerce.model.entity.RefreshToken;
import com.saas.ecommerce.model.entity.SuperAdmin;
import com.saas.ecommerce.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByClientAndRevokedFalseAndExpiryDateAfter(Client client, LocalDateTime now);

    Optional<RefreshToken> findByUserAndRevokedFalseAndExpiryDateAfter(User user, LocalDateTime now);

    Optional<RefreshToken> findBySuperAdminAndRevokedFalseAndExpiryDateAfter(SuperAdmin superAdmin, LocalDateTime now);
}