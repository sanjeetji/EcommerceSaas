package com.saas.ecommerce.controller;

import com.saas.ecommerce.model.dto.TokenResponse;
import com.saas.ecommerce.service.RefreshTokenService;
import com.saas.ecommerce.utils.HandleApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private HandleApiResponse handleApiResponse;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest request) {
        try {
            TokenResponse response = refreshTokenService.refresh(request.refreshToken());
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, "Token refreshed successfully", response);
        } catch (RuntimeException e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during token refresh: {}", e.getMessage());
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public record TokenRefreshRequest(String refreshToken) {}
}