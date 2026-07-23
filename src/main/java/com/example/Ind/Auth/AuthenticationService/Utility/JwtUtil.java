package com.example.Ind.Auth.AuthenticationService.Utility;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtUtil {

    private final String SECRET = "dGhpc0lzQVN1cGVyU3Ryb25nUmFuZG9tSldUU2VjcmV0S2V5MTIzNDU2Nzg5";

    @Getter
    private final SecretKey secretKey = Keys.hmacShaKeyFor(SECRET.getBytes());

    public String generateAuthId_JWT(String token) {

        return Jwts.builder()
                .subject(token)
                .claim("status", "new")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Map<String, Object> decodePayload(String token) {

        DecodedJWT decodedJWT = JWT.decode(token);

        Map<String, Object> claimsMap = new ConcurrentHashMap<>();

        decodedJWT.getClaims().forEach((key, claim) -> {
            if (claim.as(Object.class) != null) {
                claimsMap.put(key, claim.as(Object.class));
            }
        });

        return claimsMap;
    }
}
