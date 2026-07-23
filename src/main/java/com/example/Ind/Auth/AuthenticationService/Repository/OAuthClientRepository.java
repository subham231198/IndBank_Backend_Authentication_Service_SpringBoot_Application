package com.example.Ind.Auth.AuthenticationService.Repository;


import com.example.Ind.Auth.AuthenticationService.Entity.OAuthClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface OAuthClientRepository extends JpaRepository<OAuthClientEntity, Long> {
    Optional<OAuthClientEntity> findByClientId(String clientId);
}