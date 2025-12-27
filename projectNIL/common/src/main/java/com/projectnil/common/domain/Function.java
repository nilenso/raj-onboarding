package com.projectnil.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "functions", indexes= {
        @Index(name = "idx_functions_name", columnList = "name"),
        @Index(name = "idx_function_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Function{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable=false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable=false, length=255)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "language", nullable=false, length=50)
    private String language;

    @Column(name = "source", nullable=false, columnDefinition = "text")
    private String source;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "wasm_binary")
    private byte[] wasmBinary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable=false)
    @Builder.Default
    private FunctionStatus status = FunctionStatus.PENDING;

    @Column(name = "compile_error", columnDefinition = "text")
    private String compileError;

    @CreationTimestamp
    @Column(name = "created_at", updatable=false, nullable=false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable=false)
    private LocalDateTime updatedAt;
}
