package com.tex.cloud_task_manager.Security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.tex.cloud_task_manager.Config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

     
    private final JwtProperties jwtProps;
    
    
      private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProps.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {

        return buildToken(userDetails.getUsername());
    }

    private String buildToken(String userName){

           Instant now = Instant.now();

        return Jwts.builder()
                .subject(userName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProps.accessTokenExpirationMinutes(), ChronoUnit.MINUTES)))
                .signWith(getSigningKey())
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

     public String extractExpiration(String token) {

        String expiration = extractAllClaims(token).getExpiration().toString();
        return expiration;
    } 

    
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateToken(String email) {
        return buildToken(email);
    }
}