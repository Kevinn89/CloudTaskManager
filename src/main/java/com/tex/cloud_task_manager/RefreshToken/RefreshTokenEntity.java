package com.tex.cloud_task_manager.RefreshToken;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private  Long id;
        @Column(nullable = false)
        private long userId;
        @Column(name = "token_hash", nullable = false)
        private String tokenHash;
        @Column(nullable = false)
        private boolean revoked;
        @Column(nullable = false)
        private LocalDateTime expiresAt;
        @Column(nullable = false)
        private LocalDateTime createdAt;
        @Column(nullable = true)
        private LocalDateTime lastUsedAt;
        @Column(nullable = true)
        private LocalDateTime revokedAt;
        @Transient
        private String rawToken;
        



}
