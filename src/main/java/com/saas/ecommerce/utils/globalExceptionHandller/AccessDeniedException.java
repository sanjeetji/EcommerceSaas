package com.saas.ecommerce.utils.globalExceptionHandller;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
