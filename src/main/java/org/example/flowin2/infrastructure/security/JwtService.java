package org.example.flowin2.infrastructure.security;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.example.flowin2.domain.usuario.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey secretKey;
    private static final long EXPIRATION_TIME = 86400000; // 1 day

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Usuario usuario) {
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("id", usuario.getId())
                .claim("rol", usuario.getTipo().name())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey)
                .compact();
    }

    public String extractUserName(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenExpired(String token) {
        Date expirationDate = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expirationDate.before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return (userDetails.getUsername().equals(extractUserName(token)) && !isTokenExpired(token));
    }

    public Long extractUserId(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("id", Long.class);
    }

    public String extractRol(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("rol", String.class);
    }
}
