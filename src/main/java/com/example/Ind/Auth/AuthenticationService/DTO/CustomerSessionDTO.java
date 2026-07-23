package com.example.Ind.Auth.AuthenticationService.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerSessionDTO implements Serializable {

    private String customerSessionId;
    private String customerId;
    private String channel;
    private String authLevel;
    private String groupMemberId;
    private String correlationId;
    private String maxIdleExpirationTime;
    private String issuedAt;
    private String expiresAt;
}
