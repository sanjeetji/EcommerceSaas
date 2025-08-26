package com.saas.ecommerce.service;

import com.saas.ecommerce.model.dto.TokenResponse;
import com.saas.ecommerce.model.entity.RefreshToken;
import com.saas.ecommerce.model.entity.User;
import com.saas.ecommerce.model.dto.LoginDto;
import com.saas.ecommerce.model.dto.UserDto;
import com.saas.ecommerce.repository.UserRepository;
import com.saas.ecommerce.utils.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private RefreshTokenService refreshService;

    @Autowired
    private JwtService jwtService;

    public User createUser(UserDto dto) {
        User user = new User();
        user.setUsername(dto.username());
        user.setPassword(encoder.encode(dto.password()));
        user.setRoles(dto.roles());
        user.setClientId(TenantContext.getCurrentTenant());
        return repository.save(user);
    }

    public TokenResponse login(LoginDto dto) {
        User user = repository.findByUsername(dto.email());
        if (user == null || !encoder.matches(dto.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        RefreshToken refresh = refreshService.createRefreshToken(user);
        String access = jwtService.generateAccessToken(user.getUsername(), user.getClientId(), user.getRoles());
        return new TokenResponse(access, refresh.getToken());
    }

    public void deleteUser(Long id) {
        repository.deleteById(id);
    }

    @Cacheable("users")
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = repository.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("User not found");
        return user;
    }
}
