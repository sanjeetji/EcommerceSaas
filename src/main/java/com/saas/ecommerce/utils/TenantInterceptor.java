package com.saas.ecommerce.utils;

import com.saas.ecommerce.model.entity.Client;
import com.saas.ecommerce.service.ClientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final ClientService clientService;

    public TenantInterceptor(ClientService clientService) {
        this.clientService = clientService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Long clientId = TenantContext.getCurrentTenant(); // set in TokenValidationFilter
        if (clientId != null) {
            Client client = clientService.findById(clientId);
            if (client == null || !client.isActive()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        TenantContext.clear();
    }
}
