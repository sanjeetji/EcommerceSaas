package com.saas.ecommerce.controller;

import com.saas.ecommerce.model.dto.*;
import com.saas.ecommerce.model.entity.Client;
import com.saas.ecommerce.model.entity.User;
import com.saas.ecommerce.repository.ClientRepository;
import com.saas.ecommerce.service.UserService;
import com.saas.ecommerce.utils.Constant;
import com.saas.ecommerce.utils.HandleApiResponse;
import com.saas.ecommerce.utils.globalExceptionHandller.CustomBusinessException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.saas.ecommerce.utils.Constant.REGISTRATION_SUCCESS;
import static com.saas.ecommerce.utils.Constant.SUCCESS;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService service;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private HandleApiResponse handleApiResponse;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto dto) {
        try {
            if (dto.apiKey() == null || dto.apiKey().isEmpty()) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, "Missing apiKey in request body");
            }
            // Restrict SUPER_ADMIN creation to a specific apiKey or role
            if (dto.roles().toUpperCase(Locale.getDefault()).contains(Constant.ROLE_SUPER_ADMIN)) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.FORBIDDEN, "Unauthorized to create SUPER_ADMIN");
            }
            Optional<Client> clientOpt = clientRepository.findByClientApiKey(dto.apiKey());
            if (clientOpt.isEmpty()) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, "Invalid apiKey");
            }
            Long clientId = clientOpt.get().getId();
            if (clientId == null || clientId == 0L) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, "Client ID must be set for user registration");
            }
            UserRegistrationResponse user = service.createUser(dto, clientId);
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.CREATED, REGISTRATION_SUCCESS, user);
        } catch (CustomBusinessException e) {
            return handleApiResponse.handleApiFailedResponse(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/login")
    @RateLimiter(name = "loginLimiter")
    public ResponseEntity<?> login(@RequestBody UserLoginDto dto) {
        try {
            if (dto.apiKey() == null || dto.apiKey().isEmpty()) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, "Missing apiKey request body.");
            }
            Optional<Client> clientOpt = clientRepository.findByClientApiKey(dto.apiKey());
            if (clientOpt.isEmpty()) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, "Invalid apiKey");
            }
            Long clientId = clientOpt.get().getId();
            if (clientId == null || clientId == 0L) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, "Client ID must be set for user registration");
            }
            UserLoginResponse user = service.login(dto, clientId);
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.CREATED, REGISTRATION_SUCCESS, user);
        } catch (CustomBusinessException e) {
            return handleApiResponse.handleApiFailedResponse(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/user_list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> fetchUsers() {
        try {
            List<User> users = service.getUsers(null);
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, SUCCESS, users);
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.deleteUser(id);
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, "User deleted successfully", null);
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
