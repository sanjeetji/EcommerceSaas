package com.saas.ecommerce.model.dto;

public record UserLoginResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String gender,
        String dob,
        String token,
        String accessToken,
        String createdAt
) {}
