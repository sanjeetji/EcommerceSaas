package com.saas.ecommerce.model.entity;

import com.saas.ecommerce.utils.Constant;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Entity
@Table(name = "clients")
@Data
public class Client implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Setter
    @Getter
    private String roles = Constant.ROLE_CLIENT;

    @Setter
    @Getter
    @Column(name = "access_token", length = 512)
    private String accessToken; // Store JWT access token (optional, depending on your use case)
    @Setter
    @Getter
    @Column(name = "token", length = 512)
    private String token; // Refresh token (optional)

    @Column(nullable = false)
    private String password; // Hashed

    @Column(name = "client_api_key", nullable = false, unique = true)
    private String clientApiKey = UUID.randomUUID().toString(); // Unique API key

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(Constant.ROLE_CLIENT));
    }

    @Override
    public String getUsername() {
        return email; // Use email as the username for authentication
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}