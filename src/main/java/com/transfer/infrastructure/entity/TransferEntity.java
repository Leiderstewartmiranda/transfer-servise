package com.transfer.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "source_account_id", nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID sourceAccountId;

    @Column(name = "target_account_id", nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID targetAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
