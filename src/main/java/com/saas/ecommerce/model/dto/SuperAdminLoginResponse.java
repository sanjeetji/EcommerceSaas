package com.saas.ecommerce.model.dto;

public record SuperAdminLoginResponse(
        String token,
        String accessToken,
        String name,
        String email,
        String phoneNumber,
        String createdAt
) {
}
