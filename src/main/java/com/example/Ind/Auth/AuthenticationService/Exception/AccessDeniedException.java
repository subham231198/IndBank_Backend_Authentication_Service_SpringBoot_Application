package com.example.Ind.Auth.AuthenticationService.Exception;

public class AccessDeniedException extends RuntimeException {
  public AccessDeniedException(String message) {
    super(message);
  }
}
