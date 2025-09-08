package com.saas.ecommerce.utils;


public class Constant {
    public static final String REGISTRATION_SUCCESS = "Congratulation, You have register successful.";
    public static final String LOGIN_SUCCESS = "You have login successful.";
    public static final String SUCCESS = "Success";
    public static final String REGISTRATION_FAILED = "Sorry, Registration has been failed.";
    public static final String SOME_ERROR_OCCURRED = "Sorry, some error has occurred ";
    public static final String INTERNAL_SERVER_ERROR = "Internal server error";

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_CLIENT = "CLIENT";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";



    public static final String[] PUBLIC_URLS = {
            "/api/super-admin/register",
            "/api/super-admin/login",
            "/api/client/register",
            "/api/client/login",
            "/api/user/register",
            "/api/user/login",
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator/health",
            "/error",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };
}
