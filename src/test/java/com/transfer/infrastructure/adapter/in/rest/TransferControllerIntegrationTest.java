package com.transfer.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transfer.infrastructure.adapter.in.rest.dto.TransferDtos.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    static final UUID ACCOUNT_JUAN  = UUID.fromString("a1b2c3d4-0000-0000-0000-000000000001");
    static final UUID ACCOUNT_MARIA = UUID.fromString("a1b2c3d4-0000-0000-0000-000000000002");

    private String adminToken;

    @BeforeEach
    void obtenerToken() throws Exception {
        String loginBody = """
            { "username": "admin", "password": "admin123" }
            """;
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andReturn();

        var response = objectMapper.readTree(result.getResponse().getContentAsString());
        adminToken = response.get("accessToken").asText();
    }

    @Test
    @DisplayName("POST /api/v1/transfers - debe crear transferencia exitosamente")
    void createTransfer_success() throws Exception {
        TransferRequest request = new TransferRequest(
            ACCOUNT_JUAN, ACCOUNT_MARIA, BigDecimal.valueOf(500), "MXN", "Pago integración");

        mockMvc.perform(post("/api/v1/transfers")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.amount").value(500.00))
            .andExpect(jsonPath("$.currency").value("MXN"));
    }

    @Test
    @DisplayName("POST /api/v1/transfers - debe retornar 422 por fondos insuficientes")
    void createTransfer_insufficientFunds() throws Exception {
        TransferRequest request = new TransferRequest(
            ACCOUNT_JUAN, ACCOUNT_MARIA, BigDecimal.valueOf(999999), "MXN", "Excedido");

        mockMvc.perform(post("/api/v1/transfers")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{id} - debe retornar datos de cuenta")
    void getAccount_success() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}", ACCOUNT_JUAN)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ownerName").value("Juan Pérez"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{id} - debe retornar 404 si no existe")
    void getAccount_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/transfers - debe retornar 401 sin token")
    void createTransfer_noToken_returns401() throws Exception {
        TransferRequest request = new TransferRequest(
            ACCOUNT_JUAN, ACCOUNT_MARIA, BigDecimal.valueOf(100), "MXN", "Sin token");

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
