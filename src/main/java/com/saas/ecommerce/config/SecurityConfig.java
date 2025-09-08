package com.saas.ecommerce.config;

import com.saas.ecommerce.service.JwtService;
import com.saas.ecommerce.session.SessionPolicy;
import com.saas.ecommerce.session.SessionStore;
import com.saas.ecommerce.utils.Constant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.saas.ecommerce.utils.Constant.PUBLIC_URLS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public TokenValidationFilter tokenValidationFilter(
            JwtService jwtService,
            SessionStore sessionStore,
            SessionPolicy sessionPolicy,
            RoleHierarchy roleHierarchy) {
        return new TokenValidationFilter(jwtService, sessionStore, sessionPolicy, roleHierarchy);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TokenValidationFilter tokenValidationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers("/api/super-admin/**").hasRole(Constant.ROLE_SUPER_ADMIN)
                        .requestMatchers("/api/client/**").hasRole(Constant.ROLE_CLIENT)
                        .requestMatchers("/api/admin/**").hasRole(Constant.ROLE_ADMIN)
                        .requestMatchers("/api/user/**").hasRole(Constant.ROLE_USER)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(tokenValidationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(Constant.ROLE_SUPER_ADMIN).implies(Constant.ROLE_CLIENT)
                .role(Constant.ROLE_CLIENT).implies(Constant.ROLE_ADMIN)
                .role(Constant.ROLE_ADMIN).implies(Constant.ROLE_USER)
                .build();
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        var handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }
}
