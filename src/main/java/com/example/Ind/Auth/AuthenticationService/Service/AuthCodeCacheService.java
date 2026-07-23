package com.example.Ind.Auth.AuthenticationService.Service;

import com.example.Ind.Auth.AuthenticationService.DTO.CustomAuthCodeDTO;
import com.example.Ind.Auth.AuthenticationService.DTO.CustomerSessionDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AuthCodeCacheService {

    @Cacheable(cacheNames = "authCodeCache", key = "#p0")
    public CustomAuthCodeDTO getAuthCodeCache(String sessionId) {
        return null;
    }

    @CachePut(cacheNames = "authCodeCache", key = "#p0.code")
    public CustomAuthCodeDTO createAuthCodeCache(CustomAuthCodeDTO customAuthCodeDTO) {
        return customAuthCodeDTO;
    }

    @CachePut(cacheNames = "authCodeCache", key = "#p0.code")
    public CustomAuthCodeDTO updateAuthCodeCache(CustomAuthCodeDTO customAuthCodeDTO) {
        return customAuthCodeDTO;
    }

    @CacheEvict(cacheNames = "authCodeCache", key = "#p0")
    public void eviceAuthCodeCache(String code) {
    }
}
