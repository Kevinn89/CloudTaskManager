package com.tex.cloud_task_manager.Security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String SECRET =
            "8f4f4e0c7b0d0a7c2c42f69c9b0b0a6e9b8d3f6a4c7e1d2f9a0b1c2d3e4f5a6b";

    private final SecretKey signingKey = Keys.hmacShaKeyFor(
            SECRET.getBytes(StandardCharsets.UTF_8)
    );

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60 * 15)))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {

        String userName = extractAllClaims(token).getSubject();
        return userName;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String userName = extractUsername(token);

        return userName.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {

        boolean isTokenExpired = extractAllClaims(token)
                .getExpiration()
                .before(new Date());
        return isTokenExpired;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}