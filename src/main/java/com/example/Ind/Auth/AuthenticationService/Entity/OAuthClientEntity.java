package com.example.Ind.Auth.AuthenticationService.Entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "oauth_clients")
@Data
public class OAuthClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String clientId;

    private String clientSecret;

    private String grantTypes;

    private String redirectUri;

    private String scopes;
}