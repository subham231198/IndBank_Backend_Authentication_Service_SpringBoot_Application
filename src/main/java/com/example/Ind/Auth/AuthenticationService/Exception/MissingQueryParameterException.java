package com.example.Ind.Auth.AuthenticationService.Exception;

public class MissingQueryParameterException extends RuntimeException {
  public MissingQueryParameterException(String message) {
    super(message);
  }
}
