package com.example.Ind.Auth.AuthenticationService.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "username_auth")
public class UsernameAuthEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "customer_service_id", nullable = false, unique = true)
    private String customerServiceId;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false, unique = true)
    private String phone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "isProfileSuspended", nullable = false)
    private Boolean isProfileSuspended;

    @Column(name = "isProfileProvisioned", nullable = false)
    private Boolean isProfileProvisioned;

    @Column(name = "auth_level", nullable = false)
    private String auth_level;

    @Column(name = "is_profile_locked", nullable = false)
    private Boolean isProfileLocked;

    @Column(name = "is_password_locked", nullable = false)
    private Boolean isPasswordLocked;

    @Column(name = "is_otp_locked", nullable = false)
    private Boolean isOTPLocked;

    @Column(name = "failed_password_attempts", nullable = false)
    private Integer failedPasswordAttempts;

    @Column(name = "is_interstitial_page_shown", nullable = false)
    private Boolean isInterstitialPageShown;

    @Column(name = "failed_otp_attempts", nullable = false)
    private Integer failedOTPAttempts;

    @Column(name = "last_successful_login")
    private String lastSuccessfulLogin;

    @Column(name = "last_failed_login")
    private String lastFailedLogin;

}
