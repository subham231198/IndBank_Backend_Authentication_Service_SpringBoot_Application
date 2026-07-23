package com.example.Ind.Auth.AuthenticationService.JPALayerTest;

import com.example.Ind.Auth.AuthenticationService.Configuration.AuthorizationServerConfig;
import com.example.Ind.Auth.AuthenticationService.Configuration.SecurityConfig;
import com.example.Ind.Auth.AuthenticationService.Entity.UsernameAuthEntity;
import com.example.Ind.Auth.AuthenticationService.Repository.UsernameAuthRepo;
import com.example.Ind.Auth.AuthenticationService.Utility.GeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;

@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = false)
@Import({GeneratorService.class, SecurityConfig.class})
public class AddMockCustomer {

    @Autowired
    private UsernameAuthRepo usernameAuthRepo;

    @Autowired
    private GeneratorService generatorService;


    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @Rollback(false)
    public void createMockCustomer() {

        UsernameAuthEntity usernameAuthEntity = new UsernameAuthEntity();

        usernameAuthEntity.setUsername(generatorService.generateCustomerId());
        usernameAuthEntity.setCustomerServiceId(generatorService.generateCustomerServiceId());
        usernameAuthEntity.setPassword(passwordEncoder.encode("1q2w3e4r"));
        usernameAuthEntity.setPhone("john1.doe@test.com");
        usernameAuthEntity.setIsActive(true);
        usernameAuthEntity.setAuth_level("30");
        usernameAuthEntity.setIsProfileLocked(false);
        usernameAuthEntity.setIsPasswordLocked(false);
        usernameAuthEntity.setIsOTPLocked(false);
        usernameAuthEntity.setFailedPasswordAttempts(0);
        usernameAuthEntity.setIsInterstitialPageShown(false);
        usernameAuthEntity.setFailedOTPAttempts(0);
        usernameAuthEntity.setLastSuccessfulLogin(null);
        usernameAuthEntity.setLastFailedLogin(null);

        log.info(usernameAuthEntity.toString());

        UsernameAuthEntity savedEntity = usernameAuthRepo.save(usernameAuthEntity);

        Assertions.assertNotNull(savedEntity.getId());
        Assertions.assertNotNull(savedEntity.getUsername());
        Assertions.assertNotNull(savedEntity.getCustomerServiceId());
        Assertions.assertNotNull(savedEntity.getPhone());
        Assertions.assertTrue(savedEntity.getIsActive());
    }
}