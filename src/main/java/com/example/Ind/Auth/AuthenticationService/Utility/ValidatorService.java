package com.example.Ind.Auth.AuthenticationService.Utility;

import org.springframework.stereotype.Component;

@Component
public class ValidatorService {

    public Boolean validateCustomerId(String customerId) {
        return customerId != null &&
                !customerId.isBlank() &&
                customerId.length() < 30 &&
                customerId.matches("^[\\w!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?\\\\|`~]*$");
    }

    public Boolean validateCustomerPassword(String password) {
        return password.length() >= 8;
    }
}
