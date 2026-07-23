package com.example.Ind.Auth.AuthenticationService.Service;

import com.example.Ind.Auth.AuthenticationService.DTO.CustomerSessionDTO;
import com.example.Ind.Auth.AuthenticationService.Entity.UsernameAuthEntity;
import com.example.Ind.Auth.AuthenticationService.Exception.*;
import com.example.Ind.Auth.AuthenticationService.Repository.UsernameAuthRepo;
import com.example.Ind.Auth.AuthenticationService.Utility.ValidatorService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class UpdateCustomerService {

    @Autowired
    private UsernameAuthRepo usernameAuthRepo;

    @Autowired
    private CustomerSessionCacheService customerSessionCacheService;

    @Autowired
    private SessionAttributesService sessionAttributesService;

    @Autowired
    private ValidatorService validator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ResponseEntity<?> updateUsername(
            String tokenId,
            String channel,
            String existingCustomerId,
            String newCustomerId
    ){
        ResponseEntity<?> map = sessionAttributesService.getSessionAttributes(
                tokenId,
                channel
        );
        int sessionId = map.getStatusCode().value();

        if(sessionId != 200){
            throw new AccessDeniedException("Access Denied");
        }
        Map<String, Object> sessionAttributes = (Map<String, Object>) map.getBody();
        if(Boolean.FALSE.equals(sessionAttributes.get("isSessionValid"))){
            customerSessionCacheService.evictCustomerSession(tokenId);
            throw new InvalidSessionException("Session expired.");
        }

        String customerId = sessionAttributes.get("customerId").toString();
        log.info("customerId is {}", customerId);
        Optional<UsernameAuthEntity> user = usernameAuthRepo.findByUsername(customerId);
        if (!user.isPresent()) {
            throw new InvalidUsernameException("Customer not found.");
        }
        if (newCustomerId.equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "New username cannot be the same as current one!");
        }

        if(!validator.validateCustomerId(newCustomerId)){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "New username does not comply with system policy!");
        }

        UsernameAuthEntity usernameAuthEntity = user.get();
        usernameAuthEntity.setUsername(newCustomerId);
        usernameAuthRepo.save(usernameAuthEntity);

        CustomerSessionDTO customerSessionDTO = customerSessionCacheService.getCustomerSession(tokenId);
        customerSessionDTO.setCustomerId(newCustomerId);
        customerSessionCacheService.updateCustomerSession(customerSessionDTO);

        return ResponseEntity.ok(
                Map.of("message", "Customer username updated successfully!")
        );
    }

    public ResponseEntity<?> updatePassword(
            String tokenId,
            String channel,
            String newPassword
    ){
        ResponseEntity<?> map = sessionAttributesService.getSessionAttributes(
                tokenId,
                channel
        );
        int sessionId = map.getStatusCode().value();

        if(sessionId != 200){
            throw new AccessDeniedException("Access Denied");
        }
        Map<String, Object> sessionAttributes = (Map<String, Object>) map.getBody();
        if(Boolean.FALSE.equals(sessionAttributes.get("isSessionValid"))){
            customerSessionCacheService.evictCustomerSession(tokenId);
            throw new InvalidSessionException("Session expired.");
        }

        String customerId = (String) sessionAttributes.get("customerId");
        Optional<UsernameAuthEntity> user = usernameAuthRepo.findByUsername(customerId);
        if(!user.isPresent()){
            throw new InvalidUsernameException("Customer not found.");
        }

        if(!validator.validateCustomerPassword(newPassword)){
            throw new InvalidPasswordPolicyException("New password does not comply with system policy!");
        }

        if(passwordEncoder.matches(newPassword, user.get().getPassword())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "New password cannot be same as current password!");
        }

        UsernameAuthEntity usernameAuthEntity = user.get();
        usernameAuthEntity.setPassword(passwordEncoder.encode(newPassword));
        usernameAuthRepo.save(usernameAuthEntity);
        return ResponseEntity.ok(
                Map.of("message", "Customer password updated successfully!")
        );
    }
}
