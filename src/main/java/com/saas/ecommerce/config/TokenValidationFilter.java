package com.saas.ecommerce.config;

import com.saas.ecommerce.service.JwtService;
import com.saas.ecommerce.utils.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TokenValidationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public TokenValidationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (!jwtService.isTokenValid(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            Claims claims = jwtService.extractAllClaims(token);
            TenantContext.setCurrentTenant(claims.get("clientId", Long.class));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            claims.getSubject(),
                            null,
                            jwtService.getAuthorities(claims.get("roles", String.class))
                    );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // safety-net
            TenantContext.clear();
        }
    }
}
