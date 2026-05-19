package com.transfer.infrastructure.adapter.in.rest.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/login - debe retornar access y refresh token")
    void login_success() throws Exception {
        String body = """
            { "username": "admin", "password": "admin123" }
            """;

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.username").value("admin"))
            .andReturn();

        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - debe retornar 401 con credenciales inválidas")
    void login_badCredentials() throws Exception {
        String body = """
            { "username": "admin", "password": "wrongpassword" }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/transfers/account/{id} - debe retornar 401 sin token")
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/transfers/account/00000000-0000-0000-0000-000000000001"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/v1/transfers/account/{id} - debe funcionar con token válido")
    void protectedEndpoint_withValidToken_returns200() throws Exception {
        // 1. Obtener token
        String loginBody = """
            { "username": "user", "password": "user123" }
            """;
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        var loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginResponse.get("accessToken").asText();

        // 2. Usar token en endpoint protegido
        mockMvc.perform(get("/api/v1/transfers/account/a1b2c3d4-0000-0000-0000-000000000001")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - debe renovar el access token")
    void refresh_success() throws Exception {
        // 1. Login para obtener refresh token
        String loginBody = """
            { "username": "admin", "password": "admin123" }
            """;
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        var loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginResponse.get("refreshToken").asText();

        // 2. Usar refresh token para obtener nuevo access token
        String refreshBody = """
            { "refreshToken": "%s" }
            """.formatted(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
}
