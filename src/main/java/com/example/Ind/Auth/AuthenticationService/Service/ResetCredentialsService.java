package com.example.Ind.Auth.AuthenticationService.Service;

import com.example.Ind.Auth.AuthenticationService.Entity.UsernameAuthEntity;
import com.example.Ind.Auth.AuthenticationService.Exception.InvalidAdminException;
import com.example.Ind.Auth.AuthenticationService.Exception.InvalidUsernameException;
import com.example.Ind.Auth.AuthenticationService.Repository.UsernameAuthRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ResetCredentialsService {

    @Autowired
    private UsernameAuthRepo usernameAuthRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RestTemplate restTemplate;

    private boolean isUserAdmin(String username) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean hasAdminRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ADMIN"));

        boolean isSameUser = authentication.getName().equals(username);

        return hasAdminRole && isSameUser;
    }

    public ResponseEntity<?> resetPassword(
            String username,
            String password,
            String customerId
    ){

        if(!form_login(username, password)){
            throw new InvalidAdminException("Invalid admin credentials!");
        }

//        if (!isUserAdmin(username)) {
//            throw new InvalidAdminException("User is not authorized as admin!");
//        }

        Optional<UsernameAuthEntity> usernameAuthEntity = usernameAuthRepo.findByUsername(customerId);
        if(!usernameAuthEntity.isPresent()){
            throw new InvalidUsernameException("Invalid username provided!");
        }

        UsernameAuthEntity user = usernameAuthEntity.get();
        user.setPassword(passwordEncoder.encode("1q2w3e4r"));
        usernameAuthRepo.save(user);

        return new ResponseEntity<>(
                Map.of(
                        "message", "Reset Password for customerId = " + customerId +
                                " successful! New password = 1q2w3e4r"
                ), HttpStatus.OK
        );
    }

    public ResponseEntity<?> resetLockPassword(
            String username,
            String password,
            String customerId
    ){

        if(!form_login(username, password)){
            throw new InvalidAdminException("Invalid admin credentials!");
        }

//        if (!isUserAdmin(username)) {
//            throw new InvalidAdminException("User is not authorized as admin!");
//        }

        Optional<UsernameAuthEntity> usernameAuthEntity = usernameAuthRepo.findByUsername(customerId);
        if(!usernameAuthEntity.isPresent()){
            throw new InvalidUsernameException("Invalid username provided!");
        }

        UsernameAuthEntity user = usernameAuthEntity.get();
        user.setIsProfileLocked(false);
        user.setIsPasswordLocked(false);
        user.setFailedPasswordAttempts(0);
        usernameAuthRepo.save(user);

        return new ResponseEntity<>(
                Map.of(
                        "message", "Lock password count reset for customerId = " + customerId + " successful!"
                ), HttpStatus.OK
        );
    }

    public ResponseEntity<?> resetLockOTP(
            String username,
            String password,
            String customerId
    ){

        if(!form_login(username, password)){
            throw new InvalidAdminException("Invalid admin credentials!");
        }

        if (!isUserAdmin(username)) {
            throw new InvalidAdminException("User is not authorized as admin!");
        }

        Optional<UsernameAuthEntity> usernameAuthEntity = usernameAuthRepo.findByUsername(customerId);
        if(!usernameAuthEntity.isPresent()){
            throw new InvalidUsernameException("Invalid username provided!");
        }

        UsernameAuthEntity user = usernameAuthEntity.get();
        user.setIsProfileLocked(false);
        user.setIsOTPLocked(false);
        user.setFailedOTPAttempts(0);
        usernameAuthRepo.save(user);

        return new ResponseEntity<>(
                Map.of(
                        "message", "Lock OTP count reset for customerId = " + customerId + " successful!"
                ), HttpStatus.OK
        );
    }

    public ResponseEntity<?> unSuspendedProfile(
            String username,
            String password,
            String customerId
    ){

        if(!form_login(username, password)){
            throw new InvalidAdminException("Invalid admin credentials!");
        }

        if (!isUserAdmin(username)) {
            throw new InvalidAdminException("User is not authorized as admin!");
        }

        Optional<UsernameAuthEntity> usernameAuthEntity = usernameAuthRepo.findByUsername(customerId);
        if(!usernameAuthEntity.isPresent()){
            throw new InvalidUsernameException("Invalid username provided!");
        }

        UsernameAuthEntity user = usernameAuthEntity.get();
        user.setIsProfileSuspended(false);
        usernameAuthRepo.save(user);

        return new ResponseEntity<>(
                Map.of(
                        "message", "Suspension for customerId = " + customerId + " revoked successful!"
                ), HttpStatus.OK
        );
    }

    public boolean form_login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        log.info("{}, {}", username, password);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:7079/login",
                request,
                String.class
        );

        log.info("{}, {}", response.getStatusCode(), response.getBody());

        return response.getStatusCode().is3xxRedirection();
    }
}