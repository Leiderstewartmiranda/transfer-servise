package com.transfer.infrastructure.config;

import com.transfer.domain.service.TransferDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public TransferDomainService transferDomainService() {
        return new TransferDomainService();
    }
}
