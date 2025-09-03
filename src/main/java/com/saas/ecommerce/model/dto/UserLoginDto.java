package com.saas.ecommerce.model.dto;

public record UserLoginDto(
        String email,
        String password,
        String apiKey
) {}
