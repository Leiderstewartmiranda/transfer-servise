package com.transfer.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera un access token JWT para el usuario autenticado.
     */
    public String generateAccessToken(Authentication authentication) {
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
            .subject(username)
            .claim("roles", roles)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiry)
            .issuer("transfer-service")
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Genera un refresh token con mayor tiempo de vida.
     */
    public String generateRefreshToken(String username) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshExpirationMs());

        return Jwts.builder()
            .subject(username)
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(expiry)
            .issuer("transfer-service")
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Extrae el username (subject) del token.
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Valida el token y retorna true si es correcto.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT no soportado: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformado: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Firma JWT inválida: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT vacío o nulo: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Verifica que el token sea de tipo "access" (no refresh).
     */
    public boolean isAccessToken(String token) {
        Claims claims = parseClaims(token);
        return "access".equals(claims.get("type", String.class));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
