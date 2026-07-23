package com.example.Ind.Auth.AuthenticationService.Controller;

import com.example.Ind.Auth.AuthenticationService.DTO.CallbackRequest;
import com.example.Ind.Auth.AuthenticationService.Exception.AccessDeniedException;
import com.example.Ind.Auth.AuthenticationService.Exception.MissingQueryParameterException;
import com.example.Ind.Auth.AuthenticationService.Service.OTPAuthService;
import com.example.Ind.Auth.AuthenticationService.Service.PasswordAuthService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class WebAuthenticationController {

    @Autowired
    private PasswordAuthService passwordAuthService;

    @Autowired
    private OTPAuthService otpAuthService;

    @PostMapping(
            value = "/v1/api/dsp/authenticate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Parameter(description = "Api to perform customer authentication and dspSession generation")
    @Parameter(description = "X-Channel header is required to identify the channel of the request")
    @Parameter(description = "X-SessionCorrelationId header is required to identify the session correlation id of the request")
    @Parameter(description = "X-Group-Member header is required to identify the group member of the request")
    @Parameter(description = "type query parameter is required to identify the type of authentication")
    @Parameter(description = "authIndexValue query parameter is required to identify the authentication service to be used")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CallbackRequest object containing the username and password or otp for authentication")
    public ResponseEntity<?> authenticate(
             @RequestHeader(value = "X-Channel", required = true) String channel,
             @RequestHeader(value = "X-SessionCorrelationId", required = true) String sessionCorrelationId,
             @RequestHeader(value = "X-Group-Member", required = true) String groupMember,
             @RequestParam(value = "type") String type,
             @RequestParam(value = "authIndexValue") String authIndexValue,
             @RequestBody CallbackRequest callbackRequest
        ){
             if(type == null || type.isEmpty() || authIndexValue == null || authIndexValue.isEmpty()){
                 throw new MissingQueryParameterException("Missing required parameters");
             }
             if(!type.equals("service")){
                 throw new AccessDeniedException("Invalid type parameter");
             }
             if(authIndexValue.equals("passwordAuthService")){
                 log.info("Authenticating user: {} using passwordAuthService", callbackRequest);
                 return passwordAuthService.authenticate(
                         callbackRequest.callback().nameCallback().value(),
                         callbackRequest.callback().passwordCallback().value(),
                         channel,
                         sessionCorrelationId,
                         groupMember
                         );
             }
             else if(authIndexValue.equals("otpAuthService")){
                 return otpAuthService.authenticate(
                         callbackRequest.callback().nameCallback().value(),
                         callbackRequest.callback().passwordCallback().value(),
                         channel,
                         sessionCorrelationId,
                         groupMember
                         );
             }
             else{
                 throw new AccessDeniedException("Invalid authIndexValue");
             }
        }

}
