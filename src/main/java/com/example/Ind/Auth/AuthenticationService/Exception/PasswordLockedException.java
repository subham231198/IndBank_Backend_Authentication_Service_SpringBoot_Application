package com.example.Ind.Auth.AuthenticationService.Exception;

public class PasswordLockedException extends RuntimeException {
    public PasswordLockedException(String message) {
        super(message);
    }
}
