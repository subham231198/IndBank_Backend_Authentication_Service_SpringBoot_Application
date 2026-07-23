package com.example.Ind.Auth.AuthenticationService.Service;

import com.example.Ind.Auth.AuthenticationService.DTO.CustomerSessionDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CustomerSessionCacheService {

    @Cacheable(cacheNames = "customerSessionCache", key = "#p0")
    public CustomerSessionDTO getCustomerSession(String sessionId) {
        return null;
    }

    @CachePut(cacheNames = "customerSessionCache", key = "#p0.customerSessionId")
    public CustomerSessionDTO createCustomerSession(CustomerSessionDTO customerSessionDTO) {
        return customerSessionDTO;
    }

    @CachePut(cacheNames = "customerSessionCache", key = "#p0.customerSessionId")
    public CustomerSessionDTO updateCustomerSession(CustomerSessionDTO customerSessionDTO) {
        return customerSessionDTO;
    }

    @CacheEvict(cacheNames = "customerSessionCache", key = "#p0")
    public void evictCustomerSession(String sessionId) {
    }
}