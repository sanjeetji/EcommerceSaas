package com.saas.ecommerce.service;

import com.saas.ecommerce.model.dto.TokenResponse;
import com.saas.ecommerce.model.entity.Client;
import com.saas.ecommerce.model.entity.RefreshToken;
import com.saas.ecommerce.model.entity.User;
import com.saas.ecommerce.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class RefreshTokenService {

    @Value("${jwt.expiry.refresh}")
    private long refreshExpiry;

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate; // may be null in dev

    // 👇 constructor injection; RedisTemplate is optional
    public RefreshTokenService(RefreshTokenRepository repository,
                               JwtService jwtService,
                               @Nullable RedisTemplate<String, String> redisTemplate) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate; // can be null when Redis is disabled
    }

    public RefreshToken createRefreshToken(Object entity) {
        RefreshToken token = new RefreshToken();
        token.setToken(jwtService.generateRefreshToken());
        token.setExpiryDate(LocalDateTime.now().plus(refreshExpiry, ChronoUnit.SECONDS));
        token.setRevoked(false);

        if (entity instanceof Client client) {
            token.setClient(client);
        } else if (entity instanceof User user) {
            token.setUser(user);
        }
        return repository.save(token);
    }

    @Async
    public void revoke(String refreshToken) {
        Optional<RefreshToken> opt = repository.findByToken(refreshToken);
        opt.ifPresent(t -> {
            t.setRevoked(true);
            repository.save(t);

            // Best-effort blacklist write; skip if Redis is not present (dev)
            if (redisTemplate != null) {
                long ttl = Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), t.getExpiryDate()));
                try {
                    redisTemplate.opsForValue().set("blacklist:" + refreshToken, "revoked", ttl);
                } catch (Exception ignored) {
                    // avoid failing the request if Redis is down
                }
            }
        });
    }

    public TokenResponse refresh(String refreshToken) {
        // Best-effort blacklist read; skip if Redis is not present (dev)
        if (redisTemplate != null) {
            try {
                if (redisTemplate.opsForValue().get("blacklist:" + refreshToken) != null) {
                    throw new RuntimeException("Revoked token");
                }
            } catch (Exception ignored) {
                // ignore Redis errors in dev
            }
        }

        RefreshToken t = repository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (t.isRevoked() || t.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Expired token");
        }

        // rotate refresh token in DB
        String newRefresh = jwtService.generateRefreshToken();
        t.setToken(newRefresh);
        t.setExpiryDate(LocalDateTime.now().plus(refreshExpiry, ChronoUnit.SECONDS));
        repository.save(t);

        // build access token subject/claims
        String subject;
        Long clientId;
        String roles;

        if (t.getClient() != null) {
            Client c = t.getClient();
            subject = c.getEmail();
            clientId = c.getId();
            roles = "CLIENT";
        } else {
            User u = t.getUser();
            subject = u.getUsername();
            clientId = u.getClientId();
            roles = u.getRoles();
        }

        String newAccess = jwtService.generateAccessToken(subject, clientId, roles);
        return new TokenResponse(newAccess, newRefresh);
    }
}
