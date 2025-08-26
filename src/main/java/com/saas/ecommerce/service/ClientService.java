package com.saas.ecommerce.service;

import com.saas.ecommerce.model.dto.ClientRegistrationDto;
import com.saas.ecommerce.model.dto.TokenResponse;
import com.saas.ecommerce.model.entity.Client;
import com.saas.ecommerce.model.dto.LoginDto;
import com.saas.ecommerce.model.entity.RefreshToken;
import com.saas.ecommerce.repository.ClientRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    private final ClientRepository repository;
    private final PasswordEncoder encoder;
    private final RefreshTokenService refreshService;
    private final JwtService jwtService;

    public ClientService(ClientRepository repository, PasswordEncoder encoder, RefreshTokenService refreshService, JwtService jwtService) {
        this.repository = repository;
        this.encoder = encoder;
        this.refreshService = refreshService;
        this.jwtService = jwtService;
    }

    public TokenResponse register(ClientRegistrationDto dto) {
        Client client = new Client();
        client.setName(dto.name());
        client.setEmail(dto.email());
        client.setPassword(encoder.encode(dto.password()));
        client = repository.save(client);
        RefreshToken refresh = refreshService.createRefreshToken(client);
        String access = jwtService.generateAccessToken(client.getEmail(), client.getId(), "CLIENT");
        return new TokenResponse(access, refresh.getToken());
    }

    public TokenResponse login(LoginDto dto) {
        Client client = repository.findByEmail(dto.email()).orElseThrow();
        if (encoder.matches(dto.password(), client.getPassword())) {
            RefreshToken refresh = refreshService.createRefreshToken(client);
            String access = jwtService.generateAccessToken(client.getEmail(), client.getId(), "CLIENT");
            return new TokenResponse(access, refresh.getToken());
        }
        throw new RuntimeException("Invalid credentials");
    }

    @Cacheable("clients")
    public Client findById(Long id) {
        return repository.findById(id).orElse(null);
    }
}
