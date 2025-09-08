package com.saas.ecommerce.security;

import java.util.List;

public record AuthPrincipal(
        String username,
        Long clientId,
        Long userId,
        List<String> roles, // SUPER_ADMIN, CLIENT, ADMIN, USER (no ROLE_ prefix)
        String sid
) {
    public boolean isSuperAdmin() {
        return roles != null && roles.stream().anyMatch("SUPER_ADMIN"::equals);
    }
}
