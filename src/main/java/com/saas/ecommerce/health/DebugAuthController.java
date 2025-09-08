package com.saas.ecommerce.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugAuthController {
    @GetMapping("/me")
    public Map<String, Object> me() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return Map.of(
                "principal", auth == null ? null : auth.getPrincipal(),
                "authorities", auth == null ? null : auth.getAuthorities()
        );
    }
}

