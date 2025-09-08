package com.saas.ecommerce.controller;

import com.saas.ecommerce.model.dto.ClientRegistrationDto;
import com.saas.ecommerce.model.dto.ClientLoginDto;
import com.saas.ecommerce.service.ClientService;
import com.saas.ecommerce.service.JwtService;
import com.saas.ecommerce.utils.HandleApiResponse;
import com.saas.ecommerce.utils.globalExceptionHandller.CustomBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.saas.ecommerce.utils.Constant.*;

@RestController
@RequestMapping("/api/client")
public class ClientController {

    @Autowired
    private ClientService service;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private HandleApiResponse handleApiResponse;

    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody ClientRegistrationDto dto) {
        try {
            var response = service.register(dto);
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.CREATED, REGISTRATION_SUCCESS, response);
        } catch (CustomBusinessException e) {
            return handleApiResponse.handleApiFailedResponse(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody ClientLoginDto dto) {
        try {
            var response = service.login(dto);
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, LOGIN_SUCCESS, response);
        } catch (CustomBusinessException e) {
            return handleApiResponse.handleApiFailedResponse(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<?> getUserList(@RequestHeader("Authorization") String authorizationHeader) {
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
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<?> getUserById(@RequestHeader("Authorization") String authorizationHeader, @RequestParam("id") Long id) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            }
            String token = authorizationHeader.substring(7);
            Long clientId = jwtService.extractClientId(token);
            if (clientId == null || clientId == 0L) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, "Invalid client ID in token");
            }
            if (id == null || id == 0L){
                return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, "Missing or invalid user id");
            }
            var response = service.fetchUsersById(clientId,id);
            if (response == null) {
                return handleApiResponse.handleApiFailedResponse(HttpStatus.NOT_FOUND, "User not found");
            }
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, SUCCESS, response);
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Test Data is success.");
    }
}