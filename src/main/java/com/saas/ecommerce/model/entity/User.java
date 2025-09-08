package com.saas.ecommerce.model.entity;

import com.saas.ecommerce.utils.Constant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Setter @Getter @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter @Setter @Column(nullable = false, unique = true)
    private String email;

    @Setter @Getter @Column(nullable = false)
    String name;

    @Setter @Getter String phoneNumber;
    @Setter @Getter String gender;
    @Setter @Getter String dob;

    @Setter @Getter
    private String roles;

    @Setter @Getter @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Setter @Getter @Column(nullable = false)
    private String password;

    @Setter @Getter @Column(name = "access_token", length = 512)
    private String accessToken;

    @Setter @Getter @Column(name = "token", length = 512)
    private String token;

    @Setter @Getter @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Setter @Getter @Column(name = "client_id")
    private Long clientId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> new SimpleGrantedAuthority("ROLE_" + s))
                .toList();
    }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return active; }
    @Override public boolean isAccountNonLocked() { return active; }
    @Override public boolean isCredentialsNonExpired() { return active; }
    @Override public boolean isEnabled() { return active; }

    @PrePersist
    public void prePersist() {
        if (roles == null || roles.isBlank()) {
            roles = Constant.ROLE_USER;
        }
    }
}
