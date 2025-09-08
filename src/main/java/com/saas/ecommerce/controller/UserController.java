package com.saas.ecommerce.controller;

import com.saas.ecommerce.model.dto.*;
import com.saas.ecommerce.model.entity.Client;
import com.saas.ecommerce.repository.ClientRepository;
import com.saas.ecommerce.service.JwtService;
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
    @Autowired
    private JwtService jwtService;
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

    @GetMapping("/users")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> fetchUsers(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            }
            String token = authorizationHeader.substring(7);
            Long clientId = jwtService.extractClientId(token);
            if (clientId == null || clientId == 0L) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, "Invalid client ID in token");
            }
            var response = service.fetchUsers(clientId);
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, SUCCESS, response);
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> fetchUser(@RequestHeader("Authorization") String authorizationHeader, @RequestParam("id") Long userid) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            }
            String token = authorizationHeader.substring(7);
            Long clientId = jwtService.extractClientId(token);
            if (clientId == null || clientId == 0L) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, "Invalid client ID in token");
            }
            if (userid == null || userid == 0L){
                return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, "Missing or invalid user id");
            }
            var response = service.fetchUserByClientIdAndUserId(clientId,userid);
            if (response == null) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.NOT_FOUND, "User not found");
            }
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, SUCCESS, response);
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.deleteUser(id);
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, "User deleted successfully", null);
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
