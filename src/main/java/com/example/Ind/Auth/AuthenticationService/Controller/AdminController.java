package com.example.Ind.Auth.AuthenticationService.Controller;

import com.example.Ind.Auth.AuthenticationService.Service.ResetCredentialsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Operations", description = "Administrative operations for managing user credentials and account status")
public class AdminController {

    @Autowired
    private ResetCredentialsService resetCredentialsService;

    @PostMapping("/reset-password")
    @Operation(summary = "Reset user password", description = "Resets the password for a specific customer to default password '1q2w3e4r'")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successful",
                    content = @Content(schema = @Schema(example = "{\"message\": \"Reset Password for customerId = 12345 successful! New password = 1q2w3e4r\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid admin credentials"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not authorized as admin"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<?> resetPassword(
            @Parameter(description = "Admin username", required = true, example = "admin_auth@indbank.com")
            @RequestParam String username,

            @Parameter(description = "Admin password", required = true, example = "foobar12")
            @RequestParam String password,

            @Parameter(description = "Customer ID whose password needs to be reset", required = true, example = "12345")
            @RequestParam String customerId
    ) {
        return resetCredentialsService.resetPassword(username, password, customerId);
    }

    @PostMapping("/reset-lock-password")
    @Operation(summary = "Reset password lock", description = "Resets the password lock count and unlocks password for a specific customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password lock reset successful",
                    content = @Content(schema = @Schema(example = "{\"message\": \"Lock password count reset for customerId = 12345 successful!\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid admin credentials"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not authorized as admin"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<?> resetLockPassword(
            @Parameter(description = "Admin username", required = true, example = "admin_auth@indbank.com")
            @RequestParam String username,

            @Parameter(description = "Admin password", required = true, example = "foobar12")
            @RequestParam String password,

            @Parameter(description = "Customer ID whose password lock needs to be reset", required = true, example = "12345")
            @RequestParam String customerId
    ) {
        return resetCredentialsService.resetLockPassword(username, password, customerId);
    }

    @PostMapping("/reset-lock-otp")
    @Operation(summary = "Reset OTP lock", description = "Resets the OTP lock count and unlocks OTP for a specific customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP lock reset successful",
                    content = @Content(schema = @Schema(example = "{\"message\": \"Lock OTP count reset for customerId = 12345 successful!\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid admin credentials"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not authorized as admin"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<?> resetLockOTP(
            @Parameter(description = "Admin username", required = true, example = "admin_auth@indbank.com")
            @RequestParam String username,

            @Parameter(description = "Admin password", required = true, example = "foobar12")
            @RequestParam String password,

            @Parameter(description = "Customer ID whose OTP lock needs to be reset", required = true, example = "12345")
            @RequestParam String customerId
    ) {
        return resetCredentialsService.resetLockOTP(username, password, customerId);
    }

    @PostMapping("/unsuspend-profile")
    @Operation(summary = "Unsuspend user profile", description = "Removes suspension from a specific customer profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile suspension revoked successfully",
                    content = @Content(schema = @Schema(example = "{\"message\": \"Suspension for customerId = 12345 revoked successful!\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid admin credentials"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not authorized as admin"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<?> unSuspendedProfile(
            @Parameter(description = "Admin username", required = true, example = "admin_auth@indbank.com")
            @RequestParam String username,

            @Parameter(description = "Admin password", required = true, example = "foobar12")
            @RequestParam String password,

            @Parameter(description = "Customer ID whose profile needs to be unsuspended", required = true, example = "12345")
            @RequestParam String customerId
    ) {
        return resetCredentialsService.unSuspendedProfile(username, password, customerId);
    }
}