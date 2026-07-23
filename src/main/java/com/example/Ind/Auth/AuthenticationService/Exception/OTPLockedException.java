package com.example.Ind.Auth.AuthenticationService.Exception;

public class OTPLockedException extends RuntimeException {
    public OTPLockedException(String message) {
        super(message);
    }
}
