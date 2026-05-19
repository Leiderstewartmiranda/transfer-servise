package com.transfer.infrastructure.adapter.in.rest.auth;

import com.transfer.domain.model.Account;
import com.transfer.domain.model.Money;
import com.transfer.domain.port.out.AccountRepository;
import com.transfer.infrastructure.entity.UserEntity;
import com.transfer.infrastructure.repository.JpaUserRepository;
import com.transfer.infrastructure.security.JwtProperties;
import com.transfer.infrastructure.security.JwtTokenProvider;
import com.transfer.infrastructure.security.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login y renovación de tokens JWT")
public class AuthController {

    private final AuthenticationManager  authenticationManager;
    private final JwtTokenProvider       tokenProvider;
    private final JwtProperties          jwtProperties;
    private final UserDetailsServiceImpl userDetailsService;
    private final JpaUserRepository      userRepository;
    private final AccountRepository      accountRepository;
    private final PasswordEncoder        passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Retorna access token y refresh token JWT")
    public ResponseEntity<AuthDtos.LoginResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request) {

        log.info("Intento de login para usuario: {}", request.username());

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String accessToken  = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(request.username());

        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        log.info("Login exitoso para: {}", request.username());

        UUID accountId = userDetailsService.getAccountIdByUsername(request.username()).orElse(null);

        return ResponseEntity.ok(new AuthDtos.LoginResponse(
            accessToken, refreshToken, "Bearer",
            jwtProperties.getExpirationMs() / 1000,
            request.username(), roles, accountId
        ));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar nueva cuenta de usuario (saldo inicial 0 COP)")
    public ResponseEntity<Void> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("El usuario '" + request.username() + "' ya está en uso.");
        }

        Account account = new Account(
            UUID.randomUUID(),
            request.ownerName(),
            Money.of(BigDecimal.ZERO, "COP"),
            "COP",
            Account.AccountStatus.ACTIVE
        );
        Account saved = accountRepository.save(account);

        userRepository.save(UserEntity.builder()
            .id(UUID.randomUUID())
            .username(request.username())
            .password(passwordEncoder.encode(request.password()))
            .accountId(saved.getId())
            .role("ROLE_USER")
            .build());

        log.info("Usuario registrado: {}", request.username());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token usando refresh token")
    public ResponseEntity<AuthDtos.RefreshResponse> refresh(
            @Valid @RequestBody AuthDtos.RefreshRequest request) {

        String token = request.refreshToken();

        if (!tokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        String username     = tokenProvider.getUsernameFromToken(token);
        UserDetails user    = userDetailsService.loadUserByUsername(username);
        Authentication auth = new UsernamePasswordAuthenticationToken(
            user, null, user.getAuthorities());

        String newAccessToken = tokenProvider.generateAccessToken(auth);

        return ResponseEntity.ok(new AuthDtos.RefreshResponse(
            newAccessToken, "Bearer",
            jwtProperties.getExpirationMs() / 1000
        ));
    }
}
