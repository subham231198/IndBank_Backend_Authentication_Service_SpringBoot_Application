package com.example.Ind.Auth.AuthenticationService.Utility;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SessionGenerator {

    public String generateCustomSessionId() {
        StringBuffer sb = new StringBuffer();
        sb.append(UUID.randomUUID().toString().replace("-", ""));
        sb.append(UUID.randomUUID().toString().replace("-", ""));
        sb.append(".*");
        return sb.toString();
    }
}
