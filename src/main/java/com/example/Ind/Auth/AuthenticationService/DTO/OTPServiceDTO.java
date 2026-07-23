package com.example.Ind.Auth.AuthenticationService.DTO;


import java.time.Instant;

public record OTPServiceDTO(
        String customerId,

        String otp,

        Instant issuedAt,

        Instant expiry
) {
}
