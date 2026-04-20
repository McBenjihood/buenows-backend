package com.buenws.buenws_backend.API.Entity;

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
    public RefreshTokenEntity(String token, Instant created_at, Instant expires_at, UserEntity userEntity) {
        this.token = token;
        this.created_at = created_at;
        this.expires_at = expires_at;
        this.userEntity = userEntity;
    }

    //Columns
    @Column(name = "token")
    private String token;

    @Id
    @Column(name = "user_id")
    private UUID user_id;

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
        return user_id;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    public Instant getEdited_at() {
        return created_at;
    }
    public void setEdited_at(Instant created_at) {
        this.created_at = created_at;
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
