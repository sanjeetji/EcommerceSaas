package com.saas.ecommerce.utils.globalExceptionHandller;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
