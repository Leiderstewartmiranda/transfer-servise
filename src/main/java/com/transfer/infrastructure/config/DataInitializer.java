package com.transfer.infrastructure.config;

import com.transfer.infrastructure.entity.UserEntity;
import com.transfer.infrastructure.repository.JpaUserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    // UUID fijo de la cuenta de Leyder Stewart (cuenta del administrador)
    private static final UUID LEYDER_ACCOUNT_ID = UUID.fromString("a1b2c3d4-0000-0000-0000-000000000004");

    private final JpaUserRepository userRepository;
    private final PasswordEncoder   passwordEncoder;

    @PostConstruct
    public void init() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(UserEntity.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .accountId(LEYDER_ACCOUNT_ID)
                .role("ROLE_ADMIN")
                .build());
            log.info("Usuario admin creado con cuenta Leyder Stewart");
        } else {
            userRepository.findByUsername("admin").ifPresent(admin -> {
                if (admin.getAccountId() == null) {
                    admin.setAccountId(LEYDER_ACCOUNT_ID);
                    userRepository.save(admin);
                    log.info("Admin vinculado a cuenta Leyder Stewart");
                }
            });
        }
    }
}
