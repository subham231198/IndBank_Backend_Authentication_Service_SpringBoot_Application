package com.example.Ind.Auth.AuthenticationService.Service;

import com.example.Ind.Auth.AuthenticationService.DTO.CustomerSessionDTO;
import com.example.Ind.Auth.AuthenticationService.Exception.InvalidOTPException;
import com.example.Ind.Auth.AuthenticationService.Exception.InvalidUsernameException;
import com.example.Ind.Auth.AuthenticationService.Exception.OTPLockedException;
import com.example.Ind.Auth.AuthenticationService.Repository.UserOTPRepo;
import com.example.Ind.Auth.AuthenticationService.Repository.UsernameAuthRepo;
import com.example.Ind.Auth.AuthenticationService.Utility.SessionGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class OTPAuthService {

    @Autowired
    private UsernameAuthRepo usernameAuthRepo;

    @Autowired
    private CustomerSessionCacheService customerSessionService;

    @Autowired
    private SessionGenerator sessionGenerator;

    @Autowired
    private UserOTPRepo userOTPRepo;

    public ResponseEntity<?> authenticate(String username, String otp, String channel, String groupMemberId, String correlationId) {
        var userOptional = usernameAuthRepo.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new InvalidUsernameException("Invalid username.");
        }

        var user = userOptional.get();

        if (Boolean.TRUE.equals(user.getIsOTPLocked())) {
            throw new OTPLockedException("Account is locked due to multiple failed OTP attempts.");
        }

        if (!otp.equals(userOTPRepo.findByCustomerId(username).getOTP())) {
            int failedCount = user.getFailedOTPAttempts() == null ? 1 : user.getFailedPasswordAttempts() + 1;
            user.setFailedOTPAttempts(failedCount);
            user.setLastFailedLogin(Instant.now().toString());

            if (failedCount >= 3) {
                user.setIsOTPLocked(true);
                usernameAuthRepo.save(user);
                throw new OTPLockedException("Account is locked due to multiple failed OTP attempts.");
            }

            usernameAuthRepo.save(user);
            throw new InvalidOTPException("Invalid OTP.");
        }

        user.setFailedOTPAttempts(0);
        user.setIsOTPLocked(false);
        usernameAuthRepo.save(user);


        String sessionId = sessionGenerator.generateCustomSessionId();
        customerSessionService.createCustomerSession(
                new CustomerSessionDTO(
                        sessionId,
                        username,
                        channel,
                        "40",
                        groupMemberId,
                        correlationId,
                        Instant.now().plusSeconds(300).toString(),
                        Instant.now().toString(),
                        Instant.now().plusSeconds(600).toString()
                )
        );

        return ResponseEntity.ok(Map.of("sessionId", sessionId, "role", "./customer"));
    }
}
