package com.transfer.infrastructure.security;

import com.transfer.infrastructure.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final JpaUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
            .map(u -> User.builder()
                .username(u.getUsername())
                .password(u.getPassword())
                .authorities(u.getRole())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    public Optional<UUID> getAccountIdByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(u -> u.getAccountId());
    }

    public boolean isAdmin(String username) {
        return userRepository.findByUsername(username)
            .map(u -> "ROLE_ADMIN".equals(u.getRole()))
            .orElse(false);
    }
}
