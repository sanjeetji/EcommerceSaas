package com.saas.ecommerce.controller;

import com.saas.ecommerce.model.dto.*;
import com.saas.ecommerce.model.entity.Client;
import com.saas.ecommerce.model.entity.User;
import com.saas.ecommerce.service.ClientService;
import com.saas.ecommerce.service.KeyGeneratorService;
import com.saas.ecommerce.service.SuperAdminService;
import com.saas.ecommerce.service.UserService;
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

import static com.saas.ecommerce.utils.Constant.*;

@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminController {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private HandleApiResponse handleApiResponse;

    @Autowired
    private KeyGeneratorService keyGeneratorService;

    @Autowired
    private SuperAdminService service;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SuperAdminRegistrationDto dto) {
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
    @RateLimiter(name = "loginLimiter")
    public ResponseEntity<?> login(@RequestBody SuperAdminLoginDto dto) {
        try {
            var response = service.login(dto);
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, LOGIN_SUCCESS, response);
        } catch (CustomBusinessException e) {
            return handleApiResponse.handleApiFailedResponse(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/rotate-key")
    public ResponseEntity<String> rotateKeyManually() {
        keyGeneratorService.rotateKeyManually();
        return ResponseEntity.ok("JWT secret key rotated successfully");
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = service.getAllUsers();
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, SUCCESS, users);
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUsersByClientId(@RequestParam("client_id") Long clientId) {
        try {
            List<User> users = service.getUsersByClientId(clientId);
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, "SUCCESS", users);
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/clients")
    public ResponseEntity<?> getAllClients() {
        try {
            List<Client> clients = clientService.getAllClients();
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, SUCCESS, clients);
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/client")
    public ResponseEntity<?> getClientById(@RequestParam("id") Long id) {
        try {
            Client client = clientService.findById(id);
            return handleApiResponse.handleApiSuccessResponse(HttpStatus.OK, SUCCESS, client);
        } catch (Exception e) {
            return handleApiResponse.handleApiFailedResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


}