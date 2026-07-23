package com.example.Ind.Auth.AuthenticationService.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomAuthCodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String code;
    private String customerId;
    private String customerSessionId;
    private String channel;
    private Instant issuedAt;
    private Instant expiresAt;
}
