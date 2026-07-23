package com.example.Ind.Auth.AuthenticationService.Service;

import com.example.Ind.Auth.AuthenticationService.DTO.CustomerSessionDTO;
import com.example.Ind.Auth.AuthenticationService.Entity.AuditEntity;
import com.example.Ind.Auth.AuthenticationService.Exception.InvalidSessionException;
import com.example.Ind.Auth.AuthenticationService.Repository.AuditRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class LogOffService extends SessionAttributesService {

    @Autowired
    private AuditRepo auditRepo;

    @Autowired
    private CustomerSessionCacheService customerSessionCacheService;

    public ResponseEntity<?> logOff(String sessionId) {

        try{
            Optional<CustomerSessionDTO> sessionOptional = Optional.ofNullable(customerSessionCacheService.getCustomerSession(sessionId));
            if (sessionOptional == null || sessionOptional.isEmpty()) {
                throw new InvalidSessionException("Invalid session.");
            }

            if(!validSessionExpiry(sessionOptional.get().getExpiresAt())) {
                customerSessionCacheService.evictCustomerSession(sessionId);
                throw new InvalidSessionException("Session expired.");
            }

            customerSessionCacheService.evictCustomerSession(sessionId);

            AuditEntity auditEntity = new AuditEntity();
            auditEntity.setCustomerId(sessionOptional.get().getCustomerId());
            auditEntity.setFraudSessionId(sessionOptional.get().getCorrelationId());
            auditEntity.setCustomerServiceId(getCustomerServiceId(sessionOptional.get().getCustomerId()));
            auditEntity.setAuth_level(sessionOptional.get().getAuthLevel());
            auditEntity.setCustomerSessionId(sessionOptional.get().getCustomerSessionId());
            auditEntity.setChannel(sessionOptional.get().getChannel());
            auditEntity.setLoginTimeStamp(sessionOptional.get().getIssuedAt());
            auditEntity.setLogoutTimeStamp(java.time.Instant.now().toString());

            auditRepo.save(auditEntity);
            return ResponseEntity.ok(Map.of("message", "Successfully logged out from all sessions."));
        }
        catch (InvalidSessionException e) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 401);
            response.put("reason", "Unauthorized");
            response.put("message", "Error resolving user from JSON");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }
}
