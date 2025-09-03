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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Service for generating and validating JWT access tokens and refresh tokens.
 * Supports key rotation, multi-tenancy with clientId, and role-based access.
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.expiry.access}")
    private long accessExpiry; // Access token expiry in seconds

    @Autowired
    private KeyGeneratorService keyGeneratorService;

    // In-memory cache for parsed Claims within a request
    private final Map<String, Claims> claimsCache = new ConcurrentHashMap<>();

    /**
     * Gets the current signing key for JWT.
     *
     * @return SecretKey for HS256 signing
     */
    private SecretKey getCurrentSigningKey() {
        return Keys.hmacShaKeyFor(keyGeneratorService.getCurrentJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a JWT access token for a user.
     *
     * @param subject  The subject (e.g., username or email)
     * @param clientId The client ID for multi-tenancy
     * @param userId The ID for User, Admin or Super Admin
     * @param roles    Comma-separated roles (e.g., "ROLE_ADMIN,ROLE_USER")
     * @return JWT access token
     */
    public String generateAccessToken(String subject, Long clientId, Long userId, String roles, String sid) {
        if (subject == null || subject.isEmpty()) {
            throw new IllegalArgumentException("Subject cannot be null or empty");
        }
        Map<String, Object> claims = new HashMap<>();
        if (roles.equalsIgnoreCase(Constant.ROLE_USER)){
            claims.put("clientId", clientId != null ? clientId : 0L);
            claims.put("userId", userId != null ? userId : 0L);
        }else if (roles.equalsIgnoreCase(Constant.ROLE_ADMIN)){
            claims.put("clientId", clientId != null ? clientId : 0L);
            claims.put("userId", userId != null ? userId : 0L);
        }else if (roles.equalsIgnoreCase(Constant.ROLE_CLIENT)){
            claims.put("clientId", clientId != null ? clientId : 0L);
        }else if (roles.equalsIgnoreCase(Constant.ROLE_SUPER_ADMIN)){
            claims.put("userId", userId != null ? userId : 0L);
        }
        //claims.put("clientId", clientId != null ? clientId : 0L); // 0L for Super Admin
        claims.put("roles", roles);
        claims.put("sid", sid); // IMPORTANT

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiry * 1000))
                .signWith(getCurrentSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Generates a UUID-based refresh token.
     *
     * @return Refresh token
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Extracts all claims from a JWT token, trying current and old keys for validation.
     *
     * @param token The JWT token
     * @return Claims extracted from the token
     * @throws JwtException if token is invalid or no valid key is found
     */
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
            logger.debug("Current key failed for token validation, trying old keys");
            for (String oldSecret : keyGeneratorService.getOldJwtSecrets()) {
                try {
                    SecretKey oldKey = Keys.hmacShaKeyFor(oldSecret.getBytes(StandardCharsets.UTF_8));
                    return Jwts.parser()
                            .verifyWith(oldKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                } catch (JwtException ignored) {
                    logger.debug("Old key failed for token validation");
                }
            }
            logger.error("Token validation failed: {}", e.getMessage());
            throw new JwtException("Invalid token: no valid key found", e);
        }
    }

    /**
     * Extracts a specific claim from a JWT token.
     *
     * @param token     The JWT token
     * @param claimName The name of the claim to extract (e.g., "clientId", "roles", "sid")
     * @param type      The expected type of the claim (e.g., Long.class, String.class)
     * @param <T>       The type of the claim
     * @return The claim value
     * @throws JwtException if token is invalid or claim is missing/invalid
     */
    public <T> T extractClaim(String token, String claimName, Class<T> type) {
        if (token == null || token.isEmpty()) {
            logger.error("Token is null or empty");
            throw new JwtException("Invalid token: token is null or empty");
        }

        // Check cache first
        Claims claims = claimsCache.computeIfAbsent(token, this::parseClaims);

        Object claimValue = claims.get(claimName);
        if (claimValue == null) {
            logger.error("Claim {} not found in token", claimName);
            throw new JwtException("Claim " + claimName + " not found");
        }

        // Handle numeric type conversion for Long
        if (type == Long.class && (claimValue instanceof Number)) {
            return type.cast(((Number) claimValue).longValue());
        }

        if (!type.isInstance(claimValue)) {
            logger.error("Claim {} is of type {}, expected {}", claimName, claimValue.getClass().getSimpleName(), type.getSimpleName());
            throw new JwtException("Invalid claim type for " + claimName);
        }
        return type.cast(claimValue);
    }

    /**
     * Extracts all claims from a JWT token, trying current and old keys for validation.
     *
     * @param token The JWT token
     * @return Claims extracted from the token
     * @throws JwtException if token is invalid or no valid key is found
     */
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
            logger.debug("Current key failed for token validation, trying old keys");
            for (String oldSecret : keyGeneratorService.getOldJwtSecrets()) {
                try {
                    SecretKey oldKey = Keys.hmacShaKeyFor(oldSecret.getBytes(StandardCharsets.UTF_8));
                    return Jwts.parser()
                            .verifyWith(oldKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                } catch (JwtException ignored) {
                    logger.debug("Old key failed for token validation");
                }
            }
            logger.error("Token validation failed: {}", e.getMessage());
            throw new JwtException("Invalid token: no valid key found", e);
        }
    }

    /**
     * Checks if a JWT token is valid.
     *
     * @param token The JWT token
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
//            extractAllClaims(token);
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            logger.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Converts a comma-separated roles string to a list of authorities.
     *
     * @param roles Comma-separated roles (e.g., "ROLE_ADMIN,ROLE_USER")
     * @return List of SimpleGrantedAuthority
     */
    public List<SimpleGrantedAuthority> getAuthorities(String roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return Stream.of(roles.split(","))
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim()))
                .toList();
    }

    /**
     * Extracts clientId from a JWT token.
     *
     * @param token The JWT token
     * @return clientId (0L for Super Admin)
     * @throws JwtException if token is invalid
     */
    public Long extractClientId(String token) {
        return extractClaim(token, "clientId", Long.class);
    }

    /**
     * Extracts userId from a JWT token.
     *
     * @param token The JWT token
     * @return userId (0 L for Super Admin)
     * @throws JwtException if the token is invalid
     */
    public Long extractUserId(String token) {
        return extractClaim(token, "userId", Long.class);
    }

    /**
     * Extracts roles from a JWT token.
     *
     * @param token The JWT token
     * @return roles as a comma-separated string
     * @throws JwtException if token is invalid
     */
    public String extractRoles(String token) {
        return extractClaim(token, "roles", String.class);
    }

    /**
     * Extracts session ID (sid) from a JWT token.
     *
     * @param token The JWT token
     * @return session ID
     * @throws JwtException if token is invalid
     */
    public String extractSid(String token) {
        return extractClaim(token, "sid", String.class);
    }
}