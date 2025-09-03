package com.saas.ecommerce.model.entity;

import com.saas.ecommerce.utils.Constant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "super_admins")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class SuperAdmin implements UserDetails {
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
    @Column(nullable = true)
    String phoneNumber;
    @Setter
    @Getter
    String gender;
    @Setter
    @Getter
    String dob;
    @Setter
    @Getter
    private String roles = Constant.ROLE_SUPER_ADMIN;
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
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Setter
    @Getter
    @Column(nullable = false)
    private String password;
    @Override
    public Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + roles));
    }
    @Override
    public String getUsername() {
        return email; // Use email as the username for authentication
    }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        SuperAdmin that = (SuperAdmin) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}