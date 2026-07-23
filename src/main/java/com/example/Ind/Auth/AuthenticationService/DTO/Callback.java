package com.example.Ind.Auth.AuthenticationService.DTO;

public record Callback(
        CallbackItem authentication,
        CallbackItem nameCallback,
        CallbackItem passwordCallback
) {

}

