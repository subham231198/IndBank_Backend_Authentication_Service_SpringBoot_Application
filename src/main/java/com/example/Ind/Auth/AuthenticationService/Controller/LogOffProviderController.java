package com.example.Ind.Auth.AuthenticationService.Controller;


import com.example.Ind.Auth.AuthenticationService.Exception.InvalidSessionPatternException;
import com.example.Ind.Auth.AuthenticationService.Exception.MissingSessionIdException;
import com.example.Ind.Auth.AuthenticationService.Service.LogOffService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security/rest-sts")
public class LogOffProviderController {

    @Autowired
    private LogOffService logOffService;

    @PostMapping(
            value = "/logout",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Parameter(description = "Api to perform logout using a given tokenId.")
    @RequestBody(description = "CustomerSessionId is passed in request header.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid token state fields"),
            @ApiResponse(responseCode = "401", description = "Access denied - invalid action or token validation failed")
    })
    public ResponseEntity<?> logOff(
            @RequestHeader(value = "X-CustomerSessionId", required = true) String customerSessionId
    ){
        if(customerSessionId==null || customerSessionId.isEmpty()){
            throw new MissingSessionIdException("Customer SessionId is null!");
        }

        if(customerSessionId.length() < 30){
            throw new InvalidSessionPatternException("Customer Session Id is too short!");
        }

        return logOffService.logOff(customerSessionId);
    }
}
