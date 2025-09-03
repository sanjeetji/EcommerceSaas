package com.saas.ecommerce.session;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.session")
public class SessionPolicy {
    /**
     * Global default: enforce single active session?
     */
    private boolean singleActive = true;

    /**
     * Client IDs (tenant ids) that are allowed multi-login (skip sid check)
     */
    private Set<Long> multiLoginClients = new HashSet<>();

    /**
     * Usernames/emails that are allowed multi-login (skip sid check)
     */
    private Set<String> multiLoginUsers = new HashSet<>();

    public boolean enforceFor(Long clientId, String username) {
        if (!singleActive) return false; // globally off
        if (username != null && multiLoginUsers.contains(username)) return false;
        if (clientId != null && multiLoginClients.contains(clientId)) return false;
        return true; // otherwise enforce
    }

    // getters/setters
    public boolean isSingleActive() { return singleActive; }
    public void setSingleActive(boolean singleActive) { this.singleActive = singleActive; }
    public Set<Long> getMultiLoginClients() { return multiLoginClients; }
    public void setMultiLoginClients(Set<Long> multiLoginClients) { this.multiLoginClients = multiLoginClients; }
    public Set<String> getMultiLoginUsers() { return multiLoginUsers; }
    public void setMultiLoginUsers(Set<String> multiLoginUsers) { this.multiLoginUsers = multiLoginUsers; }
}
