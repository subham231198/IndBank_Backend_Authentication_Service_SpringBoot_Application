package com.example.Ind.Auth.AuthenticationService.Repository;

import com.example.Ind.Auth.AuthenticationService.Entity.AuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRepo extends JpaRepository<AuditEntity, Long> {

    List<AuditEntity> findByCustomerId(String customerId);
    List<AuditEntity> findByFraudSessionId(String fraudSessionId);
    List<AuditEntity> findByCustomerSessionId(String customerSessionId);
    List<AuditEntity> findByCustomerServiceId(String customerServiceId);
    Boolean existsByCustomerSessionId(String customerSessionId);
    Boolean existsByCustomerId(String customerId);
    Boolean existsByFraudSessionId(String fraudSessionId);
    Boolean existsByCustomerServiceId(String customerServiceId);
}
