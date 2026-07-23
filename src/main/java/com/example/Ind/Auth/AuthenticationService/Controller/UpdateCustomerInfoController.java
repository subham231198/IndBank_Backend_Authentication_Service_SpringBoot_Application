package com.example.Ind.Auth.AuthenticationService.Controller;

import com.example.Ind.Auth.AuthenticationService.DTO.SessionAttributes;
import com.example.Ind.Auth.AuthenticationService.Exception.AccessDeniedException;
import com.example.Ind.Auth.AuthenticationService.Exception.InvalidSessionPatternException;
import com.example.Ind.Auth.AuthenticationService.Service.UpdateCustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(name = "Customer Update", description = "APIs for updating customer information")
public class UpdateCustomerInfoController {

    @Autowired
    private UpdateCustomerService updateCustomerService;

    @PutMapping(
            value = "/api/customer/session",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update customer ID",
            description = "Updates the customer ID associated with a session. Only available for WEB channel with 'update' action.",
            operationId = "updateCustomerId"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer ID updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Customer ID updated successfully\", \"customerId\": \"new_customer_123\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Missing required headers or invalid parameters",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 400, \"reason\": \"Bad Request\", \"message\": \"Missing required header: customerSessionId\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid session or authentication",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 401, \"reason\": \"Unauthorized\", \"message\": \"Invalid session\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Access denied due to invalid channel, action, or session mismatch",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 403, \"reason\": \"Forbidden\", \"message\": \"Invalid channel parameter. Only 'WEB' is allowed.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - Customer or session not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 404, \"reason\": \"Not Found\", \"message\": \"Customer not found\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - customerSessionId and tokenId cannot be the same",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 409, \"reason\": \"Conflict\", \"message\": \"customerSessionId and tokenId cannot be the same\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Invalid session pattern",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 422, \"reason\": \"Unprocessable Entity\", \"message\": \"Customer Session Id is too short!\"}")
                    )
            )
    })
    public ResponseEntity<?> updateCustomerId(
            @Parameter(
                    description = "Action to perform. Only 'update' is allowed.",
                    required = true,
                    in = ParameterIn.QUERY,
                    example = "update",
                    schema = @Schema(allowableValues = {"update"}, defaultValue = "update")
            )
            @RequestParam(value = "action") String action,

            @Parameter(
                    description = "Customer session ID. Must be at least 30 characters long.",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "063819b72f704b5f86196b5b19bed3279d9ee18d3cf"
            )
            @RequestHeader(value = "X-CustomerSessionId", required = true) String customerSessionId,

            @Parameter(
                    description = "Channel type. Only 'WEB' is allowed.",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "WEB",
                    schema = @Schema(allowableValues = {
                            "WEB",
                            "MOBILE"
                    })
            )
            @RequestHeader(value = "X-Channel") String channel,

            @Parameter(
                    description = "Session attributes containing token ID",
                    required = true,
                    schema = @Schema(implementation = SessionAttributes.class)
            )
            @RequestBody SessionAttributes sessionAttributes,

            @Parameter(
                    description = "Current customer ID",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "customer_123"
            )
            @RequestHeader(value = "X-CustomerId") String customerId,

            @Parameter(
                    description = "New customer ID to update to",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "new_customer_456"
            )
            @RequestHeader(value = "X-New-CustomerId") String newUsername
    ) {
        if(!channel.equals("WEB")){
            throw new AccessDeniedException("Invalid channel parameter. Only 'WEB' is allowed.");
        }
        if(!action.equals("update")) {
            throw new AccessDeniedException("Invalid action parameter. Only 'update' is allowed.");
        }
        if(customerSessionId == null || customerSessionId.isBlank()) {
            return ResponseEntity.badRequest().body("Missing required header: customerSessionId");
        }
        if(customerSessionId.length() < 30){
            throw new InvalidSessionPatternException("Customer Session Id is too short!");
        }
        if(!customerSessionId.equals(sessionAttributes.getTokenId())) {
            throw new AccessDeniedException("customerSessionId and tokenId cannot be the same");
        }

        return updateCustomerService.updateUsername(customerSessionId, channel, customerId, newUsername);
    }

    @PutMapping(
            value = "/api/customer/password/session",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update customer ID",
            description = "Updates the customer ID associated with a session. Only available for WEB channel with 'update' action.",
            operationId = "updateCustomerId"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer ID updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Customer ID updated successfully\", \"customerId\": \"new_customer_123\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Missing required headers or invalid parameters",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 400, \"reason\": \"Bad Request\", \"message\": \"Missing required header: customerSessionId\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid session or authentication",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 401, \"reason\": \"Unauthorized\", \"message\": \"Invalid session\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Access denied due to invalid channel, action, or session mismatch",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 403, \"reason\": \"Forbidden\", \"message\": \"Invalid channel parameter. Only 'WEB' is allowed.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - Customer or session not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 404, \"reason\": \"Not Found\", \"message\": \"Customer not found\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - customerSessionId and tokenId cannot be the same",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 409, \"reason\": \"Conflict\", \"message\": \"customerSessionId and tokenId cannot be the same\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Invalid session pattern",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\": 422, \"reason\": \"Unprocessable Entity\", \"message\": \"Customer Session Id is too short!\"}")
                    )
            )
    })
    public ResponseEntity<?> updatePassword(
            @Parameter(
                    description = "Action to perform. Only 'update' is allowed.",
                    required = true,
                    in = ParameterIn.QUERY,
                    example = "update",
                    schema = @Schema(allowableValues = {"update"}, defaultValue = "update")
            )
            @RequestParam(value = "action") String action,

            @Parameter(
                    description = "Customer session ID. Must be at least 30 characters long.",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "063819b72f704b5f86196b5b19bed3279d9ee18d3cf"
            )
            @RequestHeader(value = "X-CustomerSessionId", required = true) String customerSessionId,

            @Parameter(
                    description = "Channel type. Only 'WEB' is allowed.",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "WEB",
                    schema = @Schema(allowableValues = {
                            "WEB",
                            "MOBILE"
                    })
            )
            @RequestHeader(value = "X-Channel") String channel,

            @Parameter(
                    description = "Session attributes containing token ID",
                    required = true,
                    schema = @Schema(implementation = SessionAttributes.class)
            )
            @RequestBody SessionAttributes sessionAttributes,

            @Parameter(
                    description = "New customer ID to update to",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "new_customer_456"
            )
            @RequestHeader(value = "X-New-Customer-Password") String newPassword
    ) {
        if(!channel.equals("WEB")){
            throw new AccessDeniedException("Invalid channel parameter. Only 'WEB' is allowed.");
        }
        if(!action.equals("update")) {
            throw new AccessDeniedException("Invalid action parameter. Only 'update' is allowed.");
        }
        if(customerSessionId == null || customerSessionId.isBlank()) {
            return ResponseEntity.badRequest().body("Missing required header: customerSessionId");
        }
        if(newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required header: newPassword");
        }
        if(customerSessionId.length() < 30){
            throw new InvalidSessionPatternException("Customer Session Id is too short!");
        }
        if(!customerSessionId.equals(sessionAttributes.getTokenId())) {
            throw new AccessDeniedException("customerSessionId and tokenId are not equal");
        }

        return updateCustomerService.updatePassword(
                customerSessionId,
                channel,
                newPassword
        );
    }
}