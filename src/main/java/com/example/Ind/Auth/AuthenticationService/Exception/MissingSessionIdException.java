package com.example.Ind.Auth.AuthenticationService.Exception;

public class MissingSessionIdException extends RuntimeException {
    public MissingSessionIdException(String message) {
        super(message);
    }
}
