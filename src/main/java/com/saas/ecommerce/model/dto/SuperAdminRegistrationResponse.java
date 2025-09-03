package com.saas.ecommerce.model.dto;

public record SuperAdminRegistrationResponse(
        String name,
        String email,
        String phoneNumber,
        String createdAt
) {
}
