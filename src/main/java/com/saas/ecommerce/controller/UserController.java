package com.saas.ecommerce.controller;

import com.saas.ecommerce.model.dto.TokenResponse;
import com.saas.ecommerce.model.entity.User;
import com.saas.ecommerce.model.dto.LoginDto;
import com.saas.ecommerce.model.dto.UserDto;
import com.saas.ecommerce.service.UserService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')") // Only clients create users
    public User create(@RequestBody UserDto dto) {
        return service.createUser(dto);
    }

    @PostMapping("/login")
    @RateLimiter(name = "loginLimiter")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginDto dto) {
        return ResponseEntity.ok(service.login(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.deleteUser(id);
    }
}
