package com.saas.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "currentTenant", type = Long.class))
@Filter(name = "tenantFilter", condition = "client_id = :currentTenant")
public class User implements UserDetails {
    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Getter
    @Setter
    @Column(nullable = false, unique = true)
    private String email;
    @Setter
    @Getter
    @Column(nullable = false)
    String name;
    @Setter
    @Getter
    String phoneNumber;
    @Setter
    @Getter
    String gender;
    @Setter
    @Getter
    String dob;
    @Setter
    @Getter
    private String roles;
    @Setter
    @Getter
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Setter
    @Getter
    @Column(nullable = false)
    private String password;
    @Setter
    @Getter
    @Column(name = "access_token", length = 512)
    private String accessToken; // Store JWT access token (optional, depending on your use case)
    @Setter
    @Getter
    @Column(name = "token", length = 512)
    private String token; // Refresh token (optional)
    @Setter
    @Getter
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    @Setter
    @Getter
    @Column(name = "client_id")
    private Long clientId;
    @Override
    public Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + roles));
    }
    @Override
    public String getUsername() {
        return email; // Use email as the username for authentication
    }
    @Override
    public boolean isAccountNonExpired() { return active; }
    @Override
    public boolean isAccountNonLocked() { return active; }
    @Override
    public boolean isCredentialsNonExpired() { return active; }
    @Override
    public boolean isEnabled() { return active; }

}