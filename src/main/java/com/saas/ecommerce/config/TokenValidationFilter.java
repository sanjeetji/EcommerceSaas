package com.saas.ecommerce.config;

import com.saas.ecommerce.security.AuthPrincipal;
import com.saas.ecommerce.service.JwtService;
import com.saas.ecommerce.session.SessionPolicy;
import com.saas.ecommerce.session.SessionStore;
import com.saas.ecommerce.utils.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.saas.ecommerce.utils.Constant.PUBLIC_URLS;
import static com.saas.ecommerce.utils.Constant.ROLE_SUPER_ADMIN;

public class TokenValidationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TokenValidationFilter.class);
    private final JwtService jwtService;
    private final SessionStore sessionStore;
    private final SessionPolicy sessionPolicy;
    private final List<PathPattern> publicPatterns;
    private final PathPatternParser pathPatternParser;
    private final RoleHierarchy roleHierarchy;

    public TokenValidationFilter(JwtService jwtService,
                                 SessionStore sessionStore,
                                 SessionPolicy sessionPolicy,
                                 RoleHierarchy roleHierarchy) {
        this.jwtService = jwtService;
        this.sessionStore = sessionStore;
        this.sessionPolicy = sessionPolicy;
        this.pathPatternParser = new PathPatternParser();
        this.roleHierarchy = roleHierarchy;
        this.publicPatterns = Arrays.stream(PUBLIC_URLS)
                .map(p -> p.startsWith("/") ? p : "/" + p)
                .map(pathPatternParser::parse)
                .toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        PathContainer path = PathContainer.parsePath(request.getServletPath());
        if (publicPatterns.stream().anyMatch(p -> p.matches(path)) ||
                "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = header.substring(7);
        try {
            if (!jwtService.isTokenValid(token)) {
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }

            Claims claims = jwtService.extractAllClaims(token);

            Object rolesClaim = claims.get("roles"); // "ADMIN,USER" or a list
            List<String> roleNames = toRoleList(rolesClaim);
            boolean isSuper = roleNames.contains(ROLE_SUPER_ADMIN) || roleNames.contains("ROLE_" + ROLE_SUPER_ADMIN);

            Long clientId = claims.get("clientId", Long.class); // null for super
            Long userId   = claims.get("userId",   Long.class); // optional
            String username = claims.getSubject();
            String tokenSid = claims.get("sid", String.class);

            if (!isSuper && clientId == null) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Client ID is missing in token");
                return;
            }

            // single-active-session
            if (sessionPolicy.enforceFor(clientId, username)) {
                String expectedSid = sessionStore.getSid(username);
                if (expectedSid == null || tokenSid == null || !tokenSid.equals(expectedSid)) {
                    sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Session expired or logged in elsewhere");
                    return;
                }
            }

            // Request-scoped context (for logs/metrics only)
            TenantContext.setCurrentTenant(isSuper ? null : clientId);

            // Build ROLE_* authorities and expand with hierarchy
            var baseAuths = roleNames.stream()
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            var expanded = roleHierarchy.getReachableGrantedAuthorities(baseAuths);

            var principal = new AuthPrincipal(
                    username,
                    clientId,
                    userId,
                    normalizeRoles(roleNames),
                    tokenSid
            );

            var auth = new UsernamePasswordAuthenticationToken(principal, null, expanded);
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        } finally {
            TenantContext.clear();
        }
    }

    private static List<String> toRoleList(Object rolesClaim) {
        if (rolesClaim == null) return List.of();
        if (rolesClaim instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        String s = String.valueOf(rolesClaim);
        if (s.isBlank()) return List.of();
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    private static List<String> normalizeRoles(List<String> raw) {
        return raw.stream().map(r -> r.startsWith("ROLE_") ? r.substring(5) : r).toList();
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\": false, \"message\": \"" + message + "\"}");
    }
}
