package com.saas.ecommerce.health;

import com.saas.ecommerce.service.KeyGeneratorService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@Component("jwtKey")
//@Profile("prod") // <-- only exists in prod
public class JwtKeyHealthIndicator implements HealthIndicator {

    private static final String SECRET_KEY_PATH = "jwt-secret.txt";
    private static final String REDIS_KEY = "jwt:secret";

    private final KeyGeneratorService keyService;
    private final RedisTemplate<String, String> redisTemplate; // maybe null

    public JwtKeyHealthIndicator(KeyGeneratorService keyService,
                                 ObjectProvider<RedisTemplate<String, String>> redisProvider) {
        this.keyService = keyService;
        this.redisTemplate = redisProvider.getIfAvailable(); // null in dev
    }

    @Override
    public Health health() {
        String key = keyService.getCurrentJwtSecret();
        boolean base64Valid = true;
        int decodedLen = 0;
        try {
            decodedLen = Base64.getDecoder().decode(key).length;
        } catch (Exception e) {
            base64Valid = false;
        }
        boolean fileSaved = Files.exists(Paths.get(SECRET_KEY_PATH));
        boolean redisSaved = false;
        if (redisTemplate != null) {
            try {
                redisSaved = redisTemplate.hasKey(REDIS_KEY);
            } catch (Exception ignored) {}
        }

        return Health.up()
                .withDetail("base64Length", key != null ? key.length() : 0)
                .withDetail("decodedBytes", decodedLen)
                .withDetail("base64Valid", base64Valid)
                .withDetail("fileSaved", fileSaved)
                .withDetail("redisSaved", redisSaved)
                .build();
    }
}
