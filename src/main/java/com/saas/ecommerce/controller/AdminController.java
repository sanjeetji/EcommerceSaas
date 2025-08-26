package com.saas.ecommerce.controller;

import com.saas.ecommerce.service.KeyGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private KeyGeneratorService keyGeneratorService;

    @PostMapping("/rotate-key")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rotateKeyManually() {
        keyGeneratorService.rotateKeyManually();
        return ResponseEntity.ok("JWT secret key rotated successfully");
    }
}