package com.saas.ecommerce.service;

import io.jsonwebtoken.Claims;
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
import java.util.stream.Stream;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.expiry.access}")
    private long accessExpiry;

    @Autowired
    private KeyGeneratorService keyGeneratorService;

    private SecretKey getCurrentSigningKey() {
        return Keys.hmacShaKeyFor(keyGeneratorService.getCurrentJwtSecret().getBytes(StandardCharsets.UTF_8));
    }


    public String generateAccessToken(String subject, Long clientId, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("clientId", clientId);
        claims.put("roles", roles);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiry * 1000))
                // 0.12.x: you can pass the alg explicitly, or omit it if your key implies HS256
                .signWith(getCurrentSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public Claims extractAllClaims(String token) {
        logger.debug("Validating token: {}", token);
        try {
            return Jwts.parser()                                // 0.12.x
                    .verifyWith(getCurrentSigningKey())         // SecretKey
                    .build()
                    .parseSignedClaims(token)                   // returns Jws<Claims>
                    .getPayload();                              // -> Claims
        } catch (Exception e) {
            logger.debug("Current key failed, trying old keys");
            for (String old : keyGeneratorService.getOldJwtSecrets()) {
                try {
                    SecretKey oldKey = Keys.hmacShaKeyFor(old.getBytes(StandardCharsets.UTF_8));
                    return Jwts.parser()
                            .verifyWith(oldKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                } catch (Exception ignored) {
                    logger.debug("Old key failed.");
                }
            }
            logger.error("Token validation failed", e);
            throw new RuntimeException("Invalid token: no valid key found", e);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            logger.error("Token validation error", e);
            return false;
        }
    }

    public List<SimpleGrantedAuthority> getAuthorities(String roles) {
        return Stream.of(roles.split(","))
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim()))
                .toList();
    }

    public Long extractClientId(String token) {
        return extractAllClaims(token).get("clientId", Long.class);
    }
}