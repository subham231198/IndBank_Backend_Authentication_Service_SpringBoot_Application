package com.example.Ind.Auth.AuthenticationService.Repository;

import com.example.Ind.Auth.AuthenticationService.Entity.UsernameAuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsernameAuthRepo extends JpaRepository<UsernameAuthEntity, String> {

    Boolean existsByUsername(String username);
    Boolean existsByPhone(String phoneNumber);
    Optional<UsernameAuthEntity> findByUsername(String username);
    Optional<UsernameAuthEntity> findByUsernameAndPassword(String username, String password);
    Optional<UsernameAuthEntity> findByPhoneAndPassword(String phoneNumber, String password);
}
