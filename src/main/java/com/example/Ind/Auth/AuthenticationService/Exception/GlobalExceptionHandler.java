package com.example.Ind.Auth.AuthenticationService.Exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(InvalidUsernameException.class)
    public ResponseEntity<?> handleInvalidUsernameException(InvalidUsernameException ex) {
       Map<String, Object> responseBody = new LinkedHashMap<>();
       responseBody.put("code", 401);
       responseBody.put("reason", "Unauthorized");
       responseBody.put("message", "Invalid username");
       return ResponseEntity.status(401).body(responseBody);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<?> handleInvalidPasswordException(InvalidPasswordException ex) {
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("code", 401);
        responseBody.put("reason", "Unauthorized");
        responseBody.put("message", "Invalid password");
        return ResponseEntity.status(401).body(responseBody);
    }

    @ExceptionHandler(InvalidOTPException.class)
    public ResponseEntity<?> handleInvalidOTPException(InvalidOTPException ex) {
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("code", 401);
        responseBody.put("reason", "Unauthorized");
        responseBody.put("message", "Invalid OTP");
        return ResponseEntity.status(401).body(responseBody);
    }

    @ExceptionHandler(PasswordLockedException.class)
    public ResponseEntity<?> handlePasswordLockedException(PasswordLockedException ex) {
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("code", 401);
        responseBody.put("reason", "Unauthorized");
        responseBody.put("message", "Password is locked");
        return ResponseEntity.status(423).body(responseBody);
    }

    @ExceptionHandler(OTPLockedException.class)
    public ResponseEntity<?> handleOTPLockedException(OTPLockedException ex) {
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("code", 401);
        responseBody.put("reason", "Unauthorized");
        responseBody.put("message", "OTP is locked");
        ex.printStackTrace();
        return ResponseEntity.status(401).body(responseBody);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("code", 401);
        responseBody.put("reason", "Unauthorized");
        responseBody.put("message", "Access denied");
        ex.printStackTrace();
        return ResponseEntity.status(401).body(responseBody);
    }

    @ExceptionHandler(MissingQueryParameterException.class)
    public ResponseEntity<?> handleMissingQueryParamException(Exception ex) {
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("code", 400);
        responseBody.put("reason", "Bad Request");
        responseBody.put("message", "Missing required query parameter");
        ex.printStackTrace();
        return ResponseEntity.status(400).body(responseBody);
    }

    @ExceptionHandler(MissingSessionIdException.class)
    public ResponseEntity<?> handleMissingSessionIdException(Exception ex){
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("code", 400);
        responseBody.put("reason", "Bad Request");
        responseBody.put("message", "tokenId cannot be empty or null!");
        ex.printStackTrace();
        return ResponseEntity.status(400).body(responseBody);
    }

    @ExceptionHandler(MissingChannelException.class)
    public ResponseEntity<?> handleMissingChannelException(Exception ex){
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("code", 400);
        responseBody.put("reason", "Bad Request");
        responseBody.put("message", "X-Channel cannot be empty or null!");
        return ResponseEntity.status(400).body(responseBody);
    }

    @ExceptionHandler(InvalidSessionException.class)
    public ResponseEntity<?> handleInvalidSessionException(InvalidSessionException ex) {
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("isSessionValid", false);
        return ResponseEntity.status(200).body(responseBody);
    }

    @ExceptionHandler(InvalidSessionPatternException.class)
    public ResponseEntity<?> handleInvalidSessionPatternException(InvalidSessionPatternException ex) {
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("code", 400);
        responseBody.put("reason", "Bad Request");
        responseBody.put("message", "Invalid tokenId provided!");
        return ResponseEntity.status(400).body(responseBody);
    }

    @ExceptionHandler(InvalidPasswordPolicyException.class)
    public ResponseEntity<?> handleInvalidPasswordPolicyException(InvalidPasswordPolicyException ex) {
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("code", 401);
        responseBody.put("reason", "Unauthorized");
        responseBody.put("message", "New password does not comply with system policy!");
        return ResponseEntity.status(400).body(responseBody);
    }

    @ExceptionHandler(InvalidAdminException.class)
    public ResponseEntity<?> handleInvalidAdminException(InvalidAdminException ex) {
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("code", 401);
        responseBody.put("reason", "Unauthorized");
        responseBody.put("message", ex.getMessage());
        return ResponseEntity.status(401).body(responseBody);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> responseBody = new LinkedHashMap<>();
        int code = 0;
        String reason = "";
        switch (ex.getStatusCode().value()){
            case 400: code = 400;
                      reason = "Bad Request";
                      break;

            case 401: code = 401;
                      reason = "Unauthorized";
                      break;

            case 403: code = 403;
                      reason = "Forbidden";
                      break;

            case 404: code = 404;
                      reason = "Not Found";
                      break;

            case 405: code = 405;
                      reason = "Method Not Allowed";
                      break;

            case 500: code = 500;
                      reason = "Internal Server Error";
                      break;

            case 501: code = 501;
                      reason = "Not Acceptable";
                      break;

            case 502: code = 502;
                      reason = "Bad Gateway";
                      break;

            case 503: code = 503;
                      reason = "Service Unavailable";
                      break;
        }
        responseBody.put("code", code);
        responseBody.put("reason", reason);
        responseBody.put("message", ex.getReason());
        return new  ResponseEntity<>(responseBody, ex.getStatusCode());
    }


}
