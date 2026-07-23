package com.example.Ind.Auth.AuthenticationService.Repository;


import com.example.Ind.Auth.AuthenticationService.Entity.UserOTPEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserOTPRepo extends JpaRepository<UserOTPEntity, Long> {
    UserOTPEntity findByCustomerId(String customerId);
}
