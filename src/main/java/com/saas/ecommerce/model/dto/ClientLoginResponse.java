package com.saas.ecommerce.model.dto;


public record ClientLoginResponse(
        String accessToken,
        String refreshToken,
        String name,
        String email,
        String phoneNumber,
        String apiKey,
        String createdAt,
        boolean active
) {}
