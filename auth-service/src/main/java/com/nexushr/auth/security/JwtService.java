package com.nexushr.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationMs;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();

        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring(5))
            .toList();

        if (roles.size() != 1) {
            throw new IllegalStateException(
                "A user must have exactly one application role"
            );
        }

        return Jwts.builder()
            .subject(userDetails.getUsername())
            .claim("role", roles.get(0))
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(signingKey)
            .compact();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject().equals(userDetails.getUsername())
            && claims.getExpiration().after(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}