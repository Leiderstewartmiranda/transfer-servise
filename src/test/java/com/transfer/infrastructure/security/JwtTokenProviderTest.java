package com.transfer.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-for-unit-tests-only-32chars!!");
        props.setExpirationMs(3_600_000L);
        props.setRefreshExpirationMs(604_800_000L);
        tokenProvider = new JwtTokenProvider(props);
    }

    private Authentication buildAuth(String username) {
        return new UsernamePasswordAuthenticationToken(
            username, null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("generateAccessToken: debe generar un token no vacío")
    void generateAccessToken_notEmpty() {
        String token = tokenProvider.generateAccessToken(buildAuth("testuser"));
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("validateToken: debe validar un token recién generado")
    void validateToken_validToken() {
        String token = tokenProvider.generateAccessToken(buildAuth("testuser"));
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: debe rechazar un token malformado")
    void validateToken_malformed() {
        assertThat(tokenProvider.validateToken("esto.no.es.un.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken: debe rechazar un token vacío")
    void validateToken_empty() {
        assertThat(tokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("getUsernameFromToken: debe extraer el username correcto")
    void getUsernameFromToken_correct() {
        String token = tokenProvider.generateAccessToken(buildAuth("juan.perez"));
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("juan.perez");
    }

    @Test
    @DisplayName("isAccessToken: access token debe retornar true")
    void isAccessToken_true() {
        String token = tokenProvider.generateAccessToken(buildAuth("admin"));
        assertThat(tokenProvider.isAccessToken(token)).isTrue();
    }

    @Test
    @DisplayName("generateRefreshToken: debe generar refresh token válido")
    void generateRefreshToken_valid() {
        String refresh = tokenProvider.generateRefreshToken("admin");
        assertThat(tokenProvider.validateToken(refresh)).isTrue();
        assertThat(tokenProvider.isAccessToken(refresh)).isFalse();
    }

    @Test
    @DisplayName("isAccessToken: refresh token debe retornar false")
    void isAccessToken_refreshReturnsFalse() {
        String refresh = tokenProvider.generateRefreshToken("admin");
        assertThat(tokenProvider.isAccessToken(refresh)).isFalse();
    }

    @Test
    @DisplayName("Token expirado debe ser inválido")
    void expiredToken_invalid() throws Exception {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret("test-secret-key-for-unit-tests-only-32chars!!");
        shortProps.setExpirationMs(1L); // 1 ms
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortProps);

        String token = shortProvider.generateAccessToken(buildAuth("admin"));
        Thread.sleep(10); // esperar que expire

        assertThat(shortProvider.validateToken(token)).isFalse();
    }
}
