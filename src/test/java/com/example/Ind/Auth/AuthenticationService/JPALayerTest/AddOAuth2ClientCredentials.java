package com.example.Ind.Auth.AuthenticationService.JPALayerTest;

import com.example.Ind.Auth.AuthenticationService.Entity.OAuthClientEntity;
import com.example.Ind.Auth.AuthenticationService.Repository.OAuthClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.Rollback;

import java.util.UUID;

@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = false)
public class AddOAuth2ClientCredentials {

    @Autowired
    private OAuthClientRepository oAuthClientRepository;

    BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    @Test
    public void createClient(){
        String clientId = UUID.randomUUID().toString();
        String clientSecret = UUID.randomUUID().toString();
        OAuthClientEntity oAuthClientEntity = new OAuthClientEntity();
        oAuthClientEntity.setClientId(clientId);
        oAuthClientEntity.setClientSecret(bCryptPasswordEncoder.encode(clientSecret));
        oAuthClientEntity.setGrantTypes("code");
        oAuthClientEntity.setScopes("openid");
        oAuthClientEntity.setRedirectUri("http://localhost:7079/callback");
        log.info("clientId = {}, clientSecret = {}", clientId, clientSecret);
        log.info(oAuthClientEntity.toString());
        oAuthClientRepository.save(oAuthClientEntity);
    }
}
