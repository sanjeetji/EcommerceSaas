package com.saas.ecommerce.model.dto;

public record ClientRegistrationResponse(
        String name,
        String email,
        String phoneNumber,
        String apiKey,
        String createdAt,
        boolean active
) {
}
