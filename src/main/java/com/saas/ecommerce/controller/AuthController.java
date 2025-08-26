package com.saas.ecommerce.controller;

import com.saas.ecommerce.model.dto.TokenResponse;
import com.saas.ecommerce.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private RefreshTokenService service;

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest req) {
        return ResponseEntity.ok(service.refresh(req.refreshToken()));
    }

    record RefreshRequest(String refreshToken) {}

    // Add logout to revoke if needed
}
