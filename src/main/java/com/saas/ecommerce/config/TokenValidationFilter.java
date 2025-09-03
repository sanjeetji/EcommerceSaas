package com.saas.ecommerce.config;

import com.saas.ecommerce.service.JwtService;
import com.saas.ecommerce.session.SessionPolicy;
import com.saas.ecommerce.session.SessionStore;
import com.saas.ecommerce.utils.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.UnknownFilterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.saas.ecommerce.utils.Constant.PUBLIC_URLS;
import static com.saas.ecommerce.utils.Constant.ROLE_SUPER_ADMIN;

public class TokenValidationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TokenValidationFilter.class);
    private final JwtService jwtService;
    private final SessionStore sessionStore;
    private final SessionPolicy sessionPolicy;
    private final List<PathPattern> publicPatterns;
    private final PathPatternParser pathPatternParser;

    @Autowired
    private EntityManager entityManager;

    public TokenValidationFilter(JwtService jwtService, SessionStore sessionStore, SessionPolicy sessionPolicy) {
        this.jwtService = jwtService;
        this.sessionStore = sessionStore;
        this.sessionPolicy = sessionPolicy;
        this.pathPatternParser = new PathPatternParser();
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
        String servletPath = request.getServletPath();
        PathContainer path = PathContainer.parsePath(servletPath);

        // Public endpoints bypass
        if (publicPatterns.stream().anyMatch(pattern -> pattern.matches(path))) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping token validation for public endpoint: {}", servletPath);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Bypass CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for URI: {}", servletPath);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = header.substring(7);
        try {
            if (!jwtService.isTokenValid(token)) {
                logger.warn("Invalid or expired token for URI: {}", servletPath);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }

            Claims claims = jwtService.extractAllClaims(token);
            String roles = claims.get("roles", String.class);
            Long clientId = claims.get("clientId", Long.class);
            String username = claims.getSubject();
            String tokenSid = claims.get("sid", String.class);

            boolean isSuper = roles != null && (roles.contains(ROLE_SUPER_ADMIN));
            if (!isSuper && clientId == null) {
                logger.error("Client ID is null in JWT for URI: {}", servletPath);
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Client ID is missing in token");
                return;
            }

            // Enforce single-active-session if applicable
            boolean enforce = sessionPolicy.enforceFor(clientId, username);
            if (enforce) {
                String expectedSid = sessionStore.getSid(username);
                if (expectedSid == null || tokenSid == null || !tokenSid.equals(expectedSid)) {
                    logger.warn("Session mismatch for user '{}': token sid={}, expected sid={}",
                            username, tokenSid, expectedSid);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "Session expired or logged in elsewhere");
                    return;
                }
            }

            // Set TenantContext and manage tenant filter
            Session session = entityManager.unwrap(Session.class);
            if (isSuper) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Super Admin access, disabling tenant filter");
                }
                TenantContext.setCurrentTenant(0L);
                try {
                    session.disableFilter("tenantFilter");
                    logger.debug("Tenant filter disabled for super admin");
                } catch (UnknownFilterException e) {
                    logger.warn("Tenant filter not found, proceeding without disabling: {}", e.getMessage());
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Client access, attempting to enable tenant filter for clientId: {}", clientId);
                }
                TenantContext.setCurrentTenant(clientId);
                try {
                    session.enableFilter("tenantFilter").setParameter("currentTenant", clientId);
                    logger.debug("Tenant filter enabled for clientId: {}", clientId);
                } catch (UnknownFilterException e) {
                    logger.error("Failed to enable tenant filter for clientId {}: {}", clientId, e.getMessage());
                    sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Tenant filter configuration error");
                    return;
                }
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    jwtService.getAuthorities(roles)
            );
            auth.setDetails(new JwtDetails(clientId, tokenSid));
            SecurityContextHolder.getContext().setAuthentication(auth);
            if (logger.isDebugEnabled()) {
                logger.debug("Token validated for user: {}, roles: {}, clientId: {}", username, roles, clientId);
            }
            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            logger.error("Token validation failed for URI: {}: {}", servletPath, e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        } finally {
            TenantContext.clear();
            try {
                Session session = entityManager.unwrap(Session.class);
                session.disableFilter("tenantFilter");
                logger.debug("Tenant filter disabled in finally block");
            } catch (UnknownFilterException e) {
                logger.warn("Tenant filter not found in finally block: {}", e.getMessage());
            }
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                String.format("{\"success\": false, \"message\": \"%s\"}", message)
        );
    }

    static class JwtDetails {
        private final Long clientId;
        private final String sid;

        public JwtDetails(Long clientId, String sid) {
            this.clientId = clientId;
            this.sid = sid;
        }

        public Long getClientId() {
            return clientId;
        }

        public String getSid() {
            return sid;
        }
    }
}