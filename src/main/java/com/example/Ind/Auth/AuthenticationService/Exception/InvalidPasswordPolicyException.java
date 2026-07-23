package com.example.Ind.Auth.AuthenticationService.Exception;

public class InvalidPasswordPolicyException extends RuntimeException {
    public InvalidPasswordPolicyException(String message) {
        super(message);
    }
}
