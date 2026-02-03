package com.buenws.buenws_backend.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private String id;

    @Column(name = "token")
    private String token;

    @Column(name = "created_at")
    private LocalDateTime created_at;

    @Column(name = "created_at")
    private LocalDateTime expires_at;

    public void setId(String id) {
        this.id = id;
    }
    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }
    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public LocalDateTime getExpires_at() {
        return expires_at;
    }
    public void setExpires_at(LocalDateTime expires_at) {
        this.expires_at = expires_at;
    }
}
