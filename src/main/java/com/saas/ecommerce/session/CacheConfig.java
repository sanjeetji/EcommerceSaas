package com.saas.ecommerce.session;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("usersList", "users","clients","superAdmins");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1000));
        return cacheManager;
    }
}

/**
 * 1. CacheConfig and Automatic Invocation
 * Question: Does CacheConfig get called automatically, or do I need to use it explicitly?
 * Answer: The CacheConfig class is automatically invoked by Spring Boot because it is annotated with @Configuration. Here’s how it works:
 *
 * Spring Boot Configuration: Classes annotated with @Configuration are scanned and processed during application startup. Spring creates beans defined in these classes (e.g., cacheManager) and makes them available in the application context.
 * Automatic Invocation: The @Bean method cacheManager() is called automatically by Spring during context initialization to create a CacheManager bean. You don’t need to invoke it explicitly.
 * Usage with @Cacheable/@CacheEvict: The CacheManager is used automatically by Spring’s caching infrastructure when methods annotated with @Cacheable, @CacheEvict, or similar annotations are called. For example, in your UserService:
 *
 *
 * 2. Resolving Cannot resolve symbol 'Caffeine'
 * Question: Why am I getting Cannot resolve symbol 'Caffeine'?
 * Answer: This error occurs because the Caffeine library is not included in your project’s dependencies. The Caffeine class is part of the com.github.benmanes.caffeine:caffeine library, which is a high-performance caching library used by Spring Boot for in-memory caching.
 *
 *
 * 3. Why Use Caffeine for Caching?
 * Question: Why are we using Caffeine in CacheConfig?
 * Answer: Caffeine is used as the underlying caching provider for Spring’s CacheManager because it offers several advantages for your application:
 *
 * High Performance:
 *
 * Caffeine is a modern, high-performance in-memory caching library optimized for speed and concurrency. It outperforms older caching libraries like Guava Cache or Ehcache for most use cases.
 * It uses efficient data structures (e.g., Window TinyLFU algorithm) to minimize memory usage and maximize cache hit rates.
 *
 *
 * Spring Boot Integration:
 *
 * Spring Boot natively supports Caffeine via CaffeineCacheManager, making it easy to configure with Spring’s caching abstractions (@Cacheable, @CacheEvict).
 * Your CacheConfig creates a CaffeineCacheManager to manage caches like usersList and users, which are used in UserService.getUsers and UserService.getAllUsers.
 *
 *
 * Configuration Flexibility:
 *
 * Your configuration sets:
 *
 * expireAfterAccess(10, TimeUnit.MINUTES): Cache entries expire 10 minutes after last access, ensuring fresh data.
 * maximumSize(1000): Limits the cache to 1000 entries to control memory usage.
 *
 *
 * These settings balance performance (fewer DB queries) and data freshness (evicting stale entries).
 *
 *
 * Use Case in Your Application:
 *
 * In UserService, methods like getUsers(Long clientId) and getAllUsers() are annotated with @Cacheable. This caches the results of user list queries, reducing database load for frequent calls (e.g., /api/user/user_list, /api/client/user_list).
 * @CacheEvict on createUser and deleteUser ensures the cache is invalidated when users are added or removed, keeping data consistent.
 *
 *
 * Why Not Other Caching Solutions?:
 *
 * Guava Cache: Older, less performant, and lacks some of Caffeine’s advanced features.
 * Ehcache: More complex to configure and better suited for distributed caching, which you don’t need for simple in-memory caching.
 * Redis: Suitable for distributed systems but adds complexity and external dependency, unnecessary for your single-instance in-memory caching needs.
 *
 *
 *
 * Summary: Caffeine is used because it’s fast, integrates seamlessly with Spring Boot, and meets your application’s need for efficient in-memory caching of user lists, reducing DB queries while maintaining data freshness.
 */