package com.example.Ind.Auth.AuthenticationService.Exception;

public class InvalidOTPException extends RuntimeException {
    public InvalidOTPException(String message) {
        super(message);
    }
}
