package com.example.Ind.Auth.AuthenticationService.Service;

import com.example.Ind.Auth.AuthenticationService.DTO.CustomAccessTokenDTO;
import com.example.Ind.Auth.AuthenticationService.DTO.CustomAuthCodeDTO;
import com.example.Ind.Auth.AuthenticationService.DTO.CustomerSessionDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenCache {

    @Cacheable(cacheNames = "authCodeCache", key = "#p0")
    public CustomAccessTokenDTO getAccessTokenCache(String sessionId) {
        return null;
    }

    @CachePut(cacheNames = "authCodeCache", key = "#p0.token")
    public CustomAccessTokenDTO createAccessTokenCache(CustomAccessTokenDTO customAccessTokenDTO) {
        return customAccessTokenDTO;
    }

    @CachePut(cacheNames = "authCodeCache", key = "#p0.token")
    public CustomAccessTokenDTO updateAccessTokenCache(CustomAccessTokenDTO customAccessTokenDTO) {
        return customAccessTokenDTO;
    }

    @CacheEvict(cacheNames = "authCodeCache", key = "#p0")
    public void evictAccessTokenCache(String token) {
    }
}
