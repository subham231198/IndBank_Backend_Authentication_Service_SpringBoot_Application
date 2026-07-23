package com.example.Ind.Auth.AuthenticationService.Service;

import com.example.Ind.Auth.AuthenticationService.DTO.CustomerSessionDTO;
import com.example.Ind.Auth.AuthenticationService.Entity.UsernameAuthEntity;
import com.example.Ind.Auth.AuthenticationService.Exception.InvalidPasswordException;
import com.example.Ind.Auth.AuthenticationService.Exception.InvalidUsernameException;
import com.example.Ind.Auth.AuthenticationService.Repository.UsernameAuthRepo;
import com.example.Ind.Auth.AuthenticationService.Utility.SessionGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PasswordAuthService {

    @Autowired
    private UsernameAuthRepo usernameAuthRepo;

    @Autowired
    private SessionGenerator sessionGenerator;

    @Autowired
    private CustomerSessionCacheService customerSessionCacheService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ResponseEntity<?> authenticate(String username,
                                          String password,
                                          String channel,
                                          String groupMemberId,
                                          String correlationId) {

        Optional<UsernameAuthEntity> userOptional = usernameAuthRepo.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new InvalidUsernameException("Invalid username.");
        }

        UsernameAuthEntity user = userOptional.get();

        if (Boolean.TRUE.equals(user.getIsPasswordLocked())) {
            throw new InvalidPasswordException("Account is locked due to multiple failed login attempts.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {

            int failedCount = user.getIsPasswordLocked() == null
                    ? 1
                    : user.getFailedPasswordAttempts() + 1;

            user.setFailedPasswordAttempts(failedCount);
            user.setLastFailedLogin(Instant.now().toString());

            if (failedCount >= 3) {
                user.setIsPasswordLocked(true);
            }

            usernameAuthRepo.save(user);

            return ResponseEntity.status(401).body(
                    failedCount >= 3
                            ? invalidPasswordResponse("Account locked after 3 failed login attempts.")
                            : invalidPasswordResponse("Invalid password.")
            );
        }

        user.setFailedPasswordAttempts(0);
        user.setLastFailedLogin(null);
        user.setIsPasswordLocked(false);
        usernameAuthRepo.save(user);

        String sessionId = sessionGenerator.generateCustomSessionId();

        customerSessionCacheService.createCustomerSession(
                new CustomerSessionDTO(
                        sessionId,
                        username,
                        channel,
                        "30",
                        correlationId,
                        groupMemberId,
                        Instant.now().plusSeconds(300).toString(),
                        Instant.now().toString(),
                        Instant.now().plusSeconds(600).toString()
                )
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tokenId", sessionId);
        response.put("role", "./customer");

        log.info("User {} authenticated successfully. Session ID: {}", username, sessionId);
        return ResponseEntity.ok(response);
    }

    public Map<String, Object> invalidPasswordResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 401);
        response.put("reason", "Unauthorized");
        response.put("message", message);
        return response;
    }
}
