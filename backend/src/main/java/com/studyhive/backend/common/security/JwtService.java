package com.studyhive.backend.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(JwtProperties properties) {
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "studyhive.jwt.secret must be at least 32 bytes long for HS256; configure JWT_SECRET");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = properties.getExpirationMs();
    }

    public String generateToken(AppUserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("userId", principal.getUserId())
                .claim("role", principal.getGlobalRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /** Returns the token's subject (username) if valid, otherwise empty. */
    public java.util.Optional<String> parseUsername(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return java.util.Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }
}
