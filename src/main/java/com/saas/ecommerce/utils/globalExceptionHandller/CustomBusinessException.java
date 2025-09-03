package com.saas.ecommerce.utils.globalExceptionHandller;


import lombok.Getter;
import org.springframework.http.HttpStatus;


public class CustomBusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;
    // Return the stored message (either cause.getMessage() or errorCode.getMessage())
    @Getter
    private final String message;

    public CustomBusinessException(ErrorCode errorCode, HttpStatus status) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.status = status;
        this.message = errorCode.getMessage();
    }

    public CustomBusinessException(ErrorCode errorCode, HttpStatus status, Throwable cause) {
        super(cause != null && cause.getMessage() != null ? cause.getMessage() : errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.status = status;
        this.message = cause != null && cause.getMessage() != null ? cause.getMessage() : errorCode.getMessage();
    }

    public CustomBusinessException(ErrorCode errorCode, HttpStatus status, String message) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.status = status;
        this.message = message != null && !message.isEmpty() ? message : errorCode.getMessage();
    }

    public int getErrorCode() {
        return errorCode.getCode();
    }

    public String getErrorMessage() {
        return errorCode.getMessage();
    }

    public HttpStatus getHttpStatus() {
        return status;
    }

}


