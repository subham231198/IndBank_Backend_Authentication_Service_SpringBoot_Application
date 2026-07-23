package com.example.Ind.Auth.AuthenticationService.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_otp")
public class UserOTPEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private String customerId;

    @Column(name = "otp", nullable = false)
    private String OTP;

    @Column(name = "issued_at", nullable = false)
    private String issuedAt;

    @Column(name = "expires_at", nullable = false)
    private String expiresAt;
}
