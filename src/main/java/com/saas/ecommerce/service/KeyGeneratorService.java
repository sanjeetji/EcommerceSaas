package com.saas.ecommerce.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;           // <-- add this
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class KeyGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(KeyGeneratorService.class);
    private static final String SECRET_KEY_PATH = "jwt-secret.txt";
    private static final String REDIS_KEY = "jwt:secret";
    private static final String REDIS_OLD_KEYS = "jwt:old_secrets";

    @Value("${jwt.rotation.enabled:false}")
    private boolean rotationEnabled;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.rotation.old-key-ttl:86400}")
    private long oldKeyTtl;

    private final RedisTemplate<String, String> redisTemplate; // may be null in dev
    private String currentKey;

    // 👇 Make RedisTemplate optional so dev profile can start without Redis
    public KeyGeneratorService(@Nullable RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            currentKey = jwtSecret;
            logger.info("JWT secret provided; rotation/storage disabled.");
            return;
        }

        String storedKey = loadKey();
        if (storedKey == null || storedKey.isEmpty()) {
            logger.info("No JWT secret found in Redis/file, generating a new one...");
            currentKey = generateJwtSecret();
            saveKey(currentKey);
            logger.info("New JWT secret generated and persisted.");
        } else {
            currentKey = storedKey;
            logger.info("JWT secret loaded from Redis/file.");
        }
    }

    private String generateJwtSecret() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            keyGen.init(256);
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            logger.error("Failed to generate JWT secret", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }

    private void saveKey(String key) {
        // Best-effort Redis
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(REDIS_KEY, key);
                logger.info("Saved JWT secret to Redis");
            }
        } catch (Exception e) {
            logger.warn("Could not save JWT secret to Redis (continuing with file): {}", e.getMessage());
        }

        // File
        try {
            Files.writeString(Paths.get(SECRET_KEY_PATH), key);
            logger.info("Saved JWT secret to file: {}", SECRET_KEY_PATH);
        } catch (IOException e) {
            logger.error("Failed to save JWT secret to file", e);
            throw new RuntimeException("Key saving failed", e);
        }
    }

    private String loadKey() {
        // Try Redis
        try {
            if (redisTemplate != null) {
                String redisKey = redisTemplate.opsForValue().get(REDIS_KEY);
                if (redisKey != null && !redisKey.isEmpty()) {
                    logger.debug("Loaded JWT secret from Redis");
                    return redisKey;
                }
            }
        } catch (Exception e) {
            logger.warn("Could not read JWT secret from Redis: {}", e.getMessage());
        }

        // Try file
        try {
            if (Files.exists(Paths.get(SECRET_KEY_PATH))) {
                logger.debug("Loaded JWT secret from file");
                return Files.readString(Paths.get(SECRET_KEY_PATH));
            }
        } catch (IOException e) {
            logger.warn("No key found in file: {}", SECRET_KEY_PATH);
        }
        return null;
    }

    @Scheduled(cron = "${jwt.rotation.cron:0 0 0 1 */6 ?}")
    public void rotateKey() {
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            logger.debug("jwt.secret configured and rotation disabled — skipping.");
            return;
        }
        if (!rotationEnabled) {
            logger.debug("Rotation disabled — skipping.");
            return;
        }

        logger.info("Rotating JWT secret key...");
        if (currentKey != null) {
            try {
                if (redisTemplate != null) {
                    redisTemplate.opsForSet().add(REDIS_OLD_KEYS, currentKey);
                    redisTemplate.expire(REDIS_OLD_KEYS, oldKeyTtl, TimeUnit.SECONDS);
                    logger.info("Stored old key in Redis with TTL: {} seconds", oldKeyTtl);
                }
            } catch (Exception e) {
                logger.warn("Could not store old key in Redis: {}", e.getMessage());
            }
        }

        currentKey = generateJwtSecret();
        saveKey(currentKey);
        logger.info("New JWT secret key generated and saved.");
    }

    public String getCurrentJwtSecret() {
        return currentKey;
    }

    public Set<String> getOldJwtSecrets() {
        try {
            if (redisTemplate != null) {
                return redisTemplate.opsForSet().members(REDIS_OLD_KEYS);
            }
        } catch (Exception e) {
            logger.warn("Could not read old keys from Redis: {}", e.getMessage());
        }
        return Collections.emptySet();
    }

    public void rotateKeyManually() {
        rotateKey();
    }
}
