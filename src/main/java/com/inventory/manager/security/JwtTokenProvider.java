package com.inventory.manager.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {
@Value("${jwt-secret}")
private String jwtSecret;
@Value("${jwt-expiration-milliseconds}")
private Long jwtExpirationInMs;
    public String generateToken(Authentication authentication){
        String name=authentication.getName();
        Date date=new Date();
        Date expiryDate=new Date(date.getTime()+jwtExpirationInMs);
        return Jwts.builder().subject(name).issuedAt(date).expiration(expiryDate).signWith(key()).compact();

    }
    private SecretKey key(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
    public String getUsername(String token){
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload().getSubject();
    }
    public boolean validateToken(String token){
        try{
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
