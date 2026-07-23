package com.example.Ind.Auth.AuthenticationService.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "session_audit")
public class AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @Column(name = "customer_session_id", nullable = false)
    private String customerSessionId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "customer_service_id", nullable = false)
    private String customerServiceId;

    @Column(name = "login_timestamp", nullable = false)
    private String loginTimeStamp;

    @Column(name = "logout_timestamp")
    private String logoutTimeStamp;

    @Column(name = "auth_level", nullable = false)
    private String auth_level;

    @Column(name = "fraud_session_id", nullable = false)
    private String fraudSessionId;
}
