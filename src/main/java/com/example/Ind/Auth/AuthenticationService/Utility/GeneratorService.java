package com.example.Ind.Auth.AuthenticationService.Utility;

import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Component
public class GeneratorService {

    Random random = new Random();

    public String generateCustomerId(){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("IN").append(1000000000 + random.nextInt(999999999));
        return stringBuffer.toString();
    }

    public String generateCustomerServiceId(){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(UUID.randomUUID()).append("-").append(UUID.randomUUID());
        return stringBuffer.toString();
    }
}
