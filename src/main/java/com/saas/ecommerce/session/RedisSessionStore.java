package com.saas.ecommerce.session;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

public class RedisSessionStore implements SessionStore {
    private final StringRedisTemplate redis;
    private static final String KEY = "sess:user:";

    public RedisSessionStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void setSid(String username, String sid, Duration ttl) {
        redis.opsForValue().set(KEY + username, sid, ttl);
    }

    @Override
    public String getSid(String username) {
        return redis.opsForValue().get(KEY + username);
    }

    @Override
    public void clearSid(String username) {
        redis.delete(KEY + username);
    }
}
