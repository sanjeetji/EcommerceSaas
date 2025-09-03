package com.saas.ecommerce.session;

import com.saas.ecommerce.model.entity.UserSession;
import com.saas.ecommerce.repository.UserSessionRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

public class DbSessionStore implements SessionStore {
    private final UserSessionRepository repo;
    private final Duration ttl;

    public DbSessionStore(UserSessionRepository repo, Duration ttl) {
        this.repo = repo;
        this.ttl = ttl;
    }

    @Override
    @Transactional
    public void setSid(String username, String sid, Duration ignored) {
        var now = Instant.now();
        int updated = repo.updateSid(username, sid, now);
        if (updated == 0) {
            UserSession us = new UserSession();
            us.setUsername(username);
            us.setSessionId(sid);
            us.setUpdatedAt(now);
            repo.save(us);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getSid(String username) {
        return repo.findByUsername(username)
                .map(us -> {
                    if (ttl != null && us.getUpdatedAt().isBefore(Instant.now().minus(ttl))) {
                        return null; // expired by TTL window
                    }
                    return us.getSessionId();
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public void clearSid(String username) {
        repo.findByUsername(username).ifPresent(repo::delete);
    }
}
