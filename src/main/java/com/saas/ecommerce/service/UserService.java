package com.saas.ecommerce.service;

import com.saas.ecommerce.model.dto.*;
import com.saas.ecommerce.model.entity.RefreshToken;
import com.saas.ecommerce.model.entity.User;
import com.saas.ecommerce.repository.UserRepository;
import com.saas.ecommerce.security.TenantGuard;
import com.saas.ecommerce.session.SessionStore;
import com.saas.ecommerce.utils.ValidateInputs;
import com.saas.ecommerce.utils.globalExceptionHandller.CustomBusinessException;
import com.saas.ecommerce.utils.globalExceptionHandller.ErrorCode;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.saas.ecommerce.security.AuthContext.*;

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

    @Autowired
    private ValidateInputs validateInputs;

    @Autowired
    private SessionStore sessionStore;

    @Autowired
    private TenantGuard tenantGuard;


    @Transactional
    public UserRegistrationResponse createUser(UserDto dto, Long clientId) {
        validateInputs.handleUserRegistration(dto,clientId);
        // If caller is authenticated and not SUPER_ADMIN, enforce same tenant
        if (!isSuperAdmin() && principal() != null) {
            tenantGuard.sameTenantOrSuper(clientId); // throws 403 if cross-tenant
        }
        if (repository.findByEmail(dto.email()) != null) {
            throw new CustomBusinessException(ErrorCode.USER_IS_ALREADY_REGISTER, HttpStatus.CONFLICT, "User already exists");
        }
        User user = new User();
        user.setEmail(dto.email());
        user.setName(dto.name());
        user.setPhoneNumber(dto.phoneNumber());
        user.setGender(dto.gender());
        user.setDob(dto.dob());
        user.setRoles(dto.roles().toUpperCase(Locale.getDefault()));
        user.setPassword(encoder.encode(dto.password()));
        user.setClientId(clientId);
        user =  repository.save(user);
        return new UserRegistrationResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getDob(),
                user.getCreatedAt().toString()
        );
    }

    @Transactional
    public UserLoginResponse login(UserLoginDto dto, Long clientId) {
        validateInputs.handleUserLogin(dto,clientId);
        User user = (User) loadUserByUsername(dto.email()); // Use loadUserByUsername for consistency
        if (user == null || !encoder.matches(dto.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        if (!user.getClientId().equals(clientId)) {
            throw new CustomBusinessException(ErrorCode.USER_IS_NOT_FOUND,HttpStatus.FORBIDDEN,"User does not belong to the specified client");
        }
        // rotate session id => invalidate all old access tokens immediately
        String sid = UUID.randomUUID().toString();
        sessionStore.setSid(dto.email(), sid, Duration.ofDays(30));
        RefreshToken refresh = refreshService.createRefreshToken(user, sid);
        String access = jwtService.generateAccessToken(user.getUsername(), user.getClientId(),user.getId(), user.getRoles(), sid);
        user.setAccessToken(access);
        user.setToken(refresh.getToken());
        user = repository.save(user);
        return new UserLoginResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getDob(),
                user.getToken(),
                user.getAccessToken(),
                user.getCreatedAt().toString()
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = repository.findByEmail(username);
        if (user == null) throw new UsernameNotFoundException("User not found with username: "+username);
        return user;
    }

    public void deleteUser(Long id) {
        var p = principal();
        var user = repository.findById(id).orElseThrow(() ->
                new CustomBusinessException(ErrorCode.USER_IS_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));

        if (!isSuperAdmin()) {
            if (p == null || p.clientId() == null || !p.clientId().equals(user.getClientId())) {
                throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, "Cross-tenant access denied");
            }
        }
        repository.delete(user);
    }

    public List<User> fetchUsers(Long clientId) {
        if (isSuperAdmin()) {
            return (clientId == null) ? repository.findAll()
                    : repository.findByClientId(clientId).orElse(Collections.emptyList());
        }
        Long mine = clientIdOrNull();
        if (mine == null) return Collections.emptyList();
        return repository.findByClientId(mine).orElse(Collections.emptyList());
    }

    public User fetchUserByClientIdAndUserId(Long clientId, Long id) {
        return repository.fetchUserByClientIdAndUserId(clientId, id).orElse(null);
    }


}