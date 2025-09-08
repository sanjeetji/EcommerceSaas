package com.saas.ecommerce.service;

import com.saas.ecommerce.model.dto.TokenResponse;
import com.saas.ecommerce.model.entity.*;
import com.saas.ecommerce.repository.*;
import com.saas.ecommerce.session.SessionPolicy;
import com.saas.ecommerce.session.SessionStore;
import com.saas.ecommerce.utils.Constant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class RefreshTokenService {

    @Value("${jwt.expiry.refresh}")
    private long refreshExpiry;

    private final JwtService jwtService;
    private final SessionStore sessionStore;
    private final SessionPolicy sessionPolicy;
    private final RefreshTokenRepository repository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final SuperAdminRepository superAdminRepository;
    private final RedisTemplate<String, String> redisTemplate; // maybe null in dev

    public RefreshTokenService(RefreshTokenRepository repository,
                               JwtService jwtService,
                               SessionStore sessionStore,
                               SessionPolicy sessionPolicy,
                               ClientRepository clientRepository,
                               UserRepository userRepository,
                               SuperAdminRepository superAdminRepository,
                               @Nullable RedisTemplate<String, String> redisTemplate) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.sessionStore = sessionStore;
        this.sessionPolicy = sessionPolicy;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.superAdminRepository = superAdminRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Create or update a refresh token for the given entity, reusing an existing non-revoked, non-expired token if available.
     */
    @Transactional
    public RefreshToken createRefreshToken(Object entity, String sid) {
        RefreshToken token;
        Optional<RefreshToken> existingToken = Optional.empty();

        // Check for existing non-revoked, non-expired token
        if (entity instanceof Client client) {
            existingToken = repository.findByClientAndRevokedFalseAndExpiryDateAfter(client, LocalDateTime.now());
        } else if (entity instanceof User user) {
            existingToken = repository.findByUserAndRevokedFalseAndExpiryDateAfter(user, LocalDateTime.now());
        } else if (entity instanceof SuperAdmin superAdmin) {
            existingToken = repository.findBySuperAdminAndRevokedFalseAndExpiryDateAfter(superAdmin, LocalDateTime.now());
        }

        if (existingToken.isPresent()) {
            // Reuse existing token
            token = existingToken.get();
            token.setToken(jwtService.generateRefreshToken());
            token.setExpiryDate(LocalDateTime.now().plus(refreshExpiry, ChronoUnit.SECONDS));
            token.setSid(sid);
        } else {
            // Create new token
            token = new RefreshToken();
            token.setToken(jwtService.generateRefreshToken());
            token.setExpiryDate(LocalDateTime.now().plus(refreshExpiry, ChronoUnit.SECONDS));
            token.setRevoked(false);
            token.setSid(sid);
            if (entity instanceof Client client) {
                token.setClient(client);
            } else if (entity instanceof User user) {
                token.setUser(user);
            } else if (entity instanceof SuperAdmin superAdmin) {
                token.setSuperAdmin(superAdmin);
            }
        }

        return repository.save(token);
    }

    /** Best-effort async revoke (DB + Redis blacklist). */
    @Async
    @Transactional
    public void revoke(String refreshToken) {
        Optional<RefreshToken> opt = repository.findByToken(refreshToken);
        opt.ifPresent(t -> {
            t.setRevoked(true);
            repository.save(t);

            if (redisTemplate != null) {
                long ttl = Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), t.getExpiryDate()));
                try {
                    redisTemplate.opsForValue().set("blacklist:" + refreshToken, "revoked", ttl);
                } catch (Exception ignored) { /* do not fail request if Redis is down */ }
            }
        });
    }

    /** Rotate refresh token & issue new access token (sid must match active session), updating the entity. */
    @Transactional
    public TokenResponse refresh(String presentedRefreshToken) {
        // Redis blacklist (optional)
        if (redisTemplate != null) {
            try {
                if (redisTemplate.opsForValue().get("blacklist:" + presentedRefreshToken) != null) {
                    throw new RuntimeException("Revoked token");
                }
            } catch (Exception ignored) { /* ignore Redis errors */ }
        }

        RefreshToken t = repository.findByToken(presentedRefreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (t.isRevoked() || t.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Expired token");
        }

        // Build subject, roles, and IDs from the owner
        String subject;
        Long clientId;
        Long userId;
        String roles;
        if (t.getClient() != null) {
            Client c = t.getClient();
            subject = c.getEmail();
            clientId = c.getId();
            userId = null; // No userId for Client
            roles = Constant.ROLE_CLIENT;
            // Update Client entity
            c.setToken(t.getToken());
            c.setAccessToken(jwtService.generateAccessToken(subject, clientId, userId, roles, t.getSid()));
            clientRepository.save(c);
        } else if (t.getUser() != null) {
            User u = t.getUser();
            subject = u.getUsername();
            userId = u.getId();
            clientId = u.getClientId();
            roles = u.getRoles();
            // Update User entity
            u.setToken(t.getToken());
            u.setAccessToken(jwtService.generateAccessToken(subject, clientId, userId, roles, t.getSid()));
            userRepository.save(u);
        }
        else if (t.getSuperAdmin() != null) {
            SuperAdmin sa = t.getSuperAdmin();
            subject = sa.getUsername();
            userId = sa.getId();
            clientId = null; // No clientId for SuperAdmin
            roles = Constant.ROLE_SUPER_ADMIN;
            // Update SuperAdmin entity
            sa.setToken(t.getToken());
            sa.setAccessToken(jwtService.generateAccessToken(subject, clientId, userId, roles, t.getSid()));
            superAdminRepository.save(sa);
        } else {
            throw new RuntimeException("Token has no owner");
        }

        // Enforce session binding: refresh token's sid must equal current server-side sid
        boolean enforce = sessionPolicy.enforceFor(clientId, subject);
        String expectedSid = sessionStore.getSid(subject);
        if (enforce) {
            if (expectedSid == null || t.getSid() == null || !t.getSid().equals(expectedSid)) {
                throw new RuntimeException("Session invalid or logged in elsewhere");
            }
        }

        // Rotate refresh token (keep same sid for this session)
        String newRefresh = jwtService.generateRefreshToken();
        t.setToken(newRefresh);
        t.setExpiryDate(LocalDateTime.now().plus(refreshExpiry, ChronoUnit.SECONDS));
        t.setSid(expectedSid); // stays same
        repository.save(t);

        // Issue new access token bound to the same sid
        String newAccess = jwtService.generateAccessToken(subject, clientId, userId, roles, expectedSid);

        // Update entity with new tokens
        if (t.getClient() != null) {
            Client c = t.getClient();
            c.setAccessToken(newAccess);
            c.setToken(newRefresh);
            clientRepository.save(c);
        } else if (t.getUser() != null) {
            User u = t.getUser();
            u.setAccessToken(newAccess);
            u.setToken(newRefresh);
            userRepository.save(u);
        }
        else if (t.getSuperAdmin() != null) {
            SuperAdmin sa = t.getSuperAdmin();
            sa.setAccessToken(newAccess);
            sa.setToken(newRefresh);
            superAdminRepository.save(sa);
        }
        return new TokenResponse(newAccess, newRefresh);
    }
}