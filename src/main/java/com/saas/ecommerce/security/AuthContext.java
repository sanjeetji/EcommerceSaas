package com.saas.ecommerce.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContext {
    private AuthContext() {}

    public static AuthPrincipal principal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.getPrincipal() instanceof AuthPrincipal p) ? p : null;
    }

    public static boolean isSuperAdmin() {
        var p = principal();
        return p != null && p.isSuperAdmin();
    }

    public static Long clientIdOrNull() {
        var p = principal();
        return p == null ? null : p.clientId();
    }
}
