package com.saas.ecommerce.repository;

import com.saas.ecommerce.model.entity.UserSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByUsername(String username);

    @Modifying
    @Query("update UserSession u set u.sessionId=:sid, u.updatedAt=:now where u.username=:username")
    int updateSid(@Param("username") String username, @Param("sid") String sid, @Param("now") Instant now);
}
