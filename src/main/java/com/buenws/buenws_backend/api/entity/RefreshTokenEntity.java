package com.buenws.buenws_backend.api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    //Columns
    @Id
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "token")
    private String token;

    @Column(name = "created_at")
    private Instant created_at;

    @Column(name = "expires_at")
    private Instant expires_at;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;


    // Getters / Setters
    public UUID getId() {
        return id;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    public Instant getCreated_at() {
        return created_at;
    }
    public Instant getExpires_at() {
        return expires_at;
    }
}
