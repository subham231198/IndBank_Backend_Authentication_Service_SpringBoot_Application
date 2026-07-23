package com.example.Ind.Auth.AuthenticationService.Controller;

import com.example.Ind.Auth.AuthenticationService.DTO.SessionAttributes;
import com.example.Ind.Auth.AuthenticationService.Exception.AccessDeniedException;
import com.example.Ind.Auth.AuthenticationService.Exception.MissingQueryParameterException;
import com.example.Ind.Auth.AuthenticationService.Service.SessionAttributesService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionAttributesController {

    @Autowired
    private SessionAttributesService sessionAttributesService;

    @PostMapping(
            value = "/api/v1/sessions",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )

    @Parameter(description = "Api to get session attributes for a given tokenId")
    @RequestBody(description = "SessionAttributes object containing the tokenId")
    public ResponseEntity<?> getSessionAttributes(
            @RequestParam String action,
            @RequestHeader String customerSessionId,
            @RequestHeader(value = "X-Channel") String channel,
            @org.springframework.web.bind.annotation.RequestBody SessionAttributes sessionAttributes
    ) {
        if(action == null || action.isEmpty()) {
            throw new MissingQueryParameterException("Missing required query parameter: action");
        }
        if(channel == null || channel.isEmpty()) {
            throw new MissingQueryParameterException("Missing required header: X-Channel");
        }
        if(!channel.equals("WEB") && !channel.equals("MOBILE")){
            throw new AccessDeniedException("Invalid channel parameter. Only 'WEB' or 'MOBILE' are allowed.");
        }
        if(!action.equals("getSessionInfo")) {
            throw new AccessDeniedException("Invalid action parameter. Only 'getSessionInfo' is allowed.");
        }
        if(customerSessionId == null || customerSessionId.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required header: customerSessionId");
        }
        if(!customerSessionId.equals(sessionAttributes.getTokenId())) {
            throw new AccessDeniedException("customerSessionId and tokenId are not equal");
        }
        return sessionAttributesService.getSessionAttributes(sessionAttributes.getTokenId(), channel);
    }
}
