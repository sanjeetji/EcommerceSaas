package com.saas.ecommerce.controller;

import com.saas.ecommerce.model.dto.ClientRegistrationDto;
import com.saas.ecommerce.model.dto.TokenResponse;
import com.saas.ecommerce.model.dto.LoginDto;
import com.saas.ecommerce.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService service;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@RequestBody ClientRegistrationDto dto) {
        return ResponseEntity.ok(service.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginDto dto) {
        return ResponseEntity.ok(service.login(dto));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Test Data is success.");
    }
}
