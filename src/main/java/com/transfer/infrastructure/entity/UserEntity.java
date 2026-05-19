package com.transfer.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "account_id", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID accountId;

    @Column(nullable = false, length = 20)
    private String role;
}
