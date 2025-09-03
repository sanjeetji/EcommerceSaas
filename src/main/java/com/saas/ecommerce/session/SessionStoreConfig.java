package com.saas.ecommerce.session;

import com.saas.ecommerce.repository.UserSessionRepository;
import com.saas.ecommerce.session.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

@Configuration
public class SessionStoreConfig {

    @Bean
    public SessionStore sessionStore(
            Optional<StringRedisTemplate> redisOpt,
            Optional<RedisConnectionFactory> redisCfOpt,
            UserSessionRepository repo,
            @Value("${app.session.store:auto}") String mode,
            @Value("${app.session.ttl-days:30}") long ttlDays
    ) {
        Logger log = LoggerFactory.getLogger(SessionStoreConfig.class);
        Duration ttl = Duration.ofDays(ttlDays);

        boolean redisUp = redisCfOpt.map(cf -> {
            try (var conn = cf.getConnection()) {
                conn.ping(); // throws if not reachable
                return true;
            } catch (Exception e) {
                log.info("Redis ping failed: {}", e.getMessage());
                return false;
            }
        }).orElse(false);

        // Modes: redis | db | auto
        if ("redis".equalsIgnoreCase(mode)) {
            if (redisUp && redisOpt.isPresent()) {
                log.info("Using RedisSessionStore (mode=redis)");
                return new RedisSessionStore(redisOpt.get());
            }
            throw new IllegalStateException("Redis selected but not available");
        }

        if ("db".equalsIgnoreCase(mode)) {
            log.info("Using DbSessionStore (mode=db)");
            return new DbSessionStore(repo, ttl);
        }

        // auto (prefer Redis if up, else DB)
        if (redisUp && redisOpt.isPresent()) {
            log.info("Using RedisSessionStore (mode=auto, redisUp=true)");
            return new RedisSessionStore(redisOpt.get());
        } else {
            log.info("Using DbSessionStore (mode=auto, redisUp=false)");
            return new DbSessionStore(repo, ttl);
        }
    }
}
