package com.example.Ind.Auth.AuthenticationService.Service;

import com.example.Ind.Auth.AuthenticationService.DTO.CustomerSessionDTO;
import com.example.Ind.Auth.AuthenticationService.Entity.UsernameAuthEntity;
import com.example.Ind.Auth.AuthenticationService.Exception.InvalidSessionException;
import com.example.Ind.Auth.AuthenticationService.Repository.UsernameAuthRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class SessionAttributesService {

    @Autowired
    private UsernameAuthRepo usernameAuthRepo;

    @Autowired
    private CustomerSessionCacheService customerSessionCacheService;

    public ResponseEntity<?> getSessionAttributes(String sessionId, String channel) {
        Optional<CustomerSessionDTO> sessionOptional = Optional.ofNullable(customerSessionCacheService.getCustomerSession(sessionId));
        if (sessionOptional == null || sessionOptional.isEmpty()) {
            throw new InvalidSessionException("Invalid session.");
        }
        if(!validSessionExpiry(sessionOptional.get().getExpiresAt())) {
            customerSessionCacheService.evictCustomerSession(sessionId);
            throw new InvalidSessionException("Session expired.");
        }
        String channelFromSession = sessionOptional.get().getChannel();
        if(channelFromSession != null && !channelFromSession.equals(channel)) {
            throw new InvalidSessionException("Channel mismatch.");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("isSessionValid", true);
        response.put("customerId", sessionOptional.get().getCustomerId());
        response.put("serviceId", getCustomerServiceId(sessionOptional.get().getCustomerId()));
        response.put("authLevel", sessionOptional.get().getAuthLevel());
        response.put("maxIdleExpirationTime", sessionOptional.get().getMaxIdleExpirationTime());
        response.put("issuedAt", sessionOptional.get().getIssuedAt());
        response.put("expiresAt", sessionOptional.get().getExpiresAt());
        response.put("correlationId", sessionOptional.get().getCorrelationId());
        response.put("channel", sessionOptional.get().getChannel());
        response.put("groupMemberId", sessionOptional.get().getGroupMemberId());
        return ResponseEntity.ok(response);
    }

    public Boolean validSessionExpiry(String expirationTime) {
        Instant expirationInstant = Instant.parse(expirationTime);
        Instant now = Instant.now();
        return now.isBefore(expirationInstant);
    }

    public String getCustomerServiceId(String customerId) {
        Optional<UsernameAuthEntity> userOptional = usernameAuthRepo.findByUsername(customerId);
        if (userOptional.isEmpty()) {
            throw new InvalidSessionException("User not found.");
        }
        return userOptional.get().getCustomerServiceId();
    }
}
