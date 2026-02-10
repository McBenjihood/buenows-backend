package com.buenws.buenws_backend.api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    //Constructors
    public RefreshTokenEntity() {
    }
    public RefreshTokenEntity(String token, UserEntity userEntity) {
        this.userEntity = userEntity;
        this.token = token;
    }
    public RefreshTokenEntity(String token, Instant edited_at, Instant expires_at, UserEntity userEntity) {
        this.token = token;
        this.edited_at = edited_at;
        this.expires_at = expires_at;
        this.userEntity = userEntity;
    }

    //Columns
    @Id
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "token")
    private String token;

    @Column(name = "edited_at")
    private Instant edited_at;

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

    public Instant getEdited_at() {
        return edited_at;
    }
    public void setEdited_at(Instant edited_at) {
        this.edited_at = edited_at;
    }

    public Instant getExpires_at() {
        return expires_at;
    }
    public void setExpires_at(Instant expires_at){
        this.expires_at = expires_at;
    }

    public UserEntity getUserEntity() {
        return userEntity;
    }
    public void setUserEntity(UserEntity userEntity) {
        this.userEntity = userEntity;
    }
}
