package com.transfer.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
