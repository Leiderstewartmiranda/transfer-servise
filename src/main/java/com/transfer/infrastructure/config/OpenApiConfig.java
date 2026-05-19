package com.transfer.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "Transfer Service API",
        version     = "1.0.0",
        description = "Microservicio de transferencia de dinero — Arquitectura Hexagonal + JWT"
    )
)
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                    .name(SECURITY_SCHEME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Ingresa el token JWT obtenido desde POST /api/v1/auth/login")
                )
            );
    }
}
