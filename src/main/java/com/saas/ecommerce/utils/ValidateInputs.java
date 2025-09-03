package com.saas.ecommerce.utils;


import com.saas.ecommerce.model.dto.*;
import com.saas.ecommerce.utils.globalExceptionHandller.CustomBusinessException;
import com.saas.ecommerce.utils.globalExceptionHandller.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ValidateInputs {

    public void handleClientRegistrationInput(ClientRegistrationDto request) {
        if (request.name() == null || request.name().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (request.email() == null || request.email().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (request.password() == null || request.password().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (request.phoneNumber() == null || request.phoneNumber().isEmpty()) {
            throw new IllegalArgumentException("Phone cannot be null or empty");
        }
    }

    public void handleClientLoginInput(ClientLoginDto request) {
        if (request.email() == null || request.email().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (request.password() == null || request.password().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
    }

    public void handleUserRegistration(UserDto dto, Long clientId) {
        if (clientId == null) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "Client ID must be set for user creation");
        }
        if (dto.name() == null || dto.name().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "name is required");
        }
        if (dto.phoneNumber() == null || dto.phoneNumber().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "Phone number is required");
        }
        if (dto.email() == null || dto.email().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (dto.gender() == null || dto.gender().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "Gender is required");
        }
        if (dto.dob() == null || dto.dob().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "DOB is required");
        }
        if (dto.password() == null || dto.password().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (dto.roles() == null || dto.roles().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "Role is required");
        }
    }

    public void handleUserLogin(UserLoginDto dto, Long clientId) {
        if (clientId == null) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "Client ID must be set for user creation");
        }
        if (dto.apiKey() == null || dto.apiKey().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "ApiKey is required");
        }
        if (dto.email() == null || dto.email().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (dto.password() == null || dto.password().isEmpty()) {
            throw new CustomBusinessException(ErrorCode.FAILED_TO_REGISTER, HttpStatus.BAD_REQUEST, "Password is required");
        }
    }

    public void handleSuperAdminRegistrationInput(SuperAdminRegistrationDto request) {
        if (request.name() == null || request.name().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (request.email() == null || request.email().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (request.password() == null || request.password().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (request.phoneNumber() == null || request.phoneNumber().isEmpty()) {
            throw new IllegalArgumentException("Phone cannot be null or empty");
        }
    }

    public void handleSuperAdminLoginInput(SuperAdminLoginDto request) {
        if (request.email() == null || request.email().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (request.password() == null || request.password().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
    }

}
