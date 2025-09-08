package com.saas.ecommerce.service;

import com.saas.ecommerce.utils.Constant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.expiry.access}")
    private long accessExpiry; // seconds

    @Autowired
    private KeyGeneratorService keyGeneratorService;

    private final Map<String, Claims> claimsCache = new ConcurrentHashMap<>();

    private SecretKey getCurrentSigningKey() {
        return Keys.hmacShaKeyFor(keyGeneratorService.getCurrentJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String subject, Long clientId, Long userId, String roles, String sid) {
        if (subject == null || subject.isEmpty()) {
            throw new IllegalArgumentException("Subject cannot be null or empty");
        }
        Map<String, Object> claims = new HashMap<>();


        if (Constant.ROLE_SUPER_ADMIN.equalsIgnoreCase(roles)) {
            claims.put("superAdminId",   userId   != null ? userId   : 0L);
        }else if (Constant.ROLE_CLIENT.equalsIgnoreCase(roles)) {
            claims.put("clientId", clientId != null ? clientId : 0L);
        }else if (Constant.ROLE_ADMIN.equalsIgnoreCase(roles)){
            claims.put("clientId", clientId != null ? clientId : 0L);
            claims.put("adminId",   userId   != null ? userId   : 0L);
        }else if (Constant.ROLE_USER.equalsIgnoreCase(roles) ) {
            claims.put("clientId", clientId != null ? clientId : 0L);
            claims.put("userId",   userId   != null ? userId   : 0L);
        }

        claims.put("roles", roles); // keep as string for compatibility ("ADMIN,USER" etc.)
        claims.put("sid",   sid);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiry * 1000))
                .signWith(getCurrentSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public Claims extractAllClaims(String token) {
        if (token == null || token.isEmpty()) {
            logger.error("Token is null or empty");
            throw new JwtException("Invalid token: token is null or empty");
        }

        try {
            return Jwts.parser()
                    .verifyWith(getCurrentSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            logger.debug("Current key failed; trying old keys");
            for (String oldSecret : keyGeneratorService.getOldJwtSecrets()) {
                try {
                    SecretKey oldKey = Keys.hmacShaKeyFor(oldSecret.getBytes(StandardCharsets.UTF_8));
                    return Jwts.parser()
                            .verifyWith(oldKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                } catch (JwtException ignored) { }
            }
            logger.error("Token validation failed: {}", e.getMessage());
            throw new JwtException("Invalid token: no valid key found", e);
        }
    }

    public <T> T extractClaim(String token, String claimName, Class<T> type) {
        if (token == null || token.isEmpty()) {
            logger.error("Token is null or empty");
            throw new JwtException("Invalid token: token is null or empty");
        }

        Claims claims = claimsCache.computeIfAbsent(token, this::parseClaims);
        Object claimValue = claims.get(claimName);
        if (claimValue == null) {
            logger.error("Claim {} not found in token", claimName);
            throw new JwtException("Claim " + claimName + " not found");
        }

        if (type == Long.class && (claimValue instanceof Number)) {
            return type.cast(((Number) claimValue).longValue());
        }
        if (!type.isInstance(claimValue)) {
            logger.error("Claim {} type mismatch: {} vs {}", claimName,
                    claimValue.getClass().getSimpleName(), type.getSimpleName());
            throw new JwtException("Invalid claim type for " + claimName);
        }
        return type.cast(claimValue);
    }

    private Claims parseClaims(String token) {
        if (token == null || token.isEmpty()) {
            logger.error("Token is null or empty");
            throw new JwtException("Invalid token: token is null or empty");
        }
        try {
            return Jwts.parser()
                    .verifyWith(getCurrentSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            for (String oldSecret : keyGeneratorService.getOldJwtSecrets()) {
                try {
                    SecretKey oldKey = Keys.hmacShaKeyFor(oldSecret.getBytes(StandardCharsets.UTF_8));
                    return Jwts.parser()
                            .verifyWith(oldKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                } catch (JwtException ignored) { }
            }
            throw new JwtException("Invalid token: no valid key found", e);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            logger.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    public List<SimpleGrantedAuthority> getAuthorities(String roles) {
        if (roles == null || roles.isEmpty()) return List.of();
        return Stream.of(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public Long extractSuperAdminId(String token) { return extractClaim(token, "superAdminId", Long.class); }
    public Long extractClientId(String token) { return extractClaim(token, "clientId", Long.class); }
    public Long extractAdminId(String token)   { return extractClaim(token, "adminId",   Long.class); }
    public Long extractUserId(String token)   { return extractClaim(token, "userId",   Long.class); }
    public String extractRoles(String token)  { return extractClaim(token, "roles",    String.class); }
    public String extractSid(String token)    { return extractClaim(token, "sid",      String.class); }
}
