package com.buenws.buenws_backend.API.Entity;

import com.buenws.buenws_backend.Util.TimeUtil;
import jakarta.persistence.*;
import org.springframework.security.core.userdetails.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reset_codes")
public class ResetCodeEntity {

    public ResetCodeEntity(){}

    public ResetCodeEntity(UserEntity userEntity){
        this.reset_code = "";
        this.verified_token = null;
        this.userEntity = userEntity;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "reset_code")
    private String reset_code;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "active")
    private boolean active;

    @Column(name = "verified_token")
    String verified_token;

    @Column(name = "updated_at")
    private Instant updated_at;

    @Column(name = "expires_at")
    private Instant expires_at;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    //Getters
    public Long getId() {
        return id;
    }
    public UserEntity getUserEntity() {
        return userEntity;
    }

    //Getters & Setters
    public String getReset_code() {
        return reset_code;
    }
    public void setReset_code(String reset_code) {
        this.reset_code = reset_code;
    }

    public int getAttempts() {
        return attempts;
    }
    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getExpires_at() {
        return expires_at;
    }
    public void setExpires_at(Instant expires_at) {
        this.expires_at = expires_at;
    }

    public Instant getUpdated_at() {
        return updated_at;
    }
    public void setUpdated_at(Instant updated_at) {
        this.updated_at = updated_at;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    public boolean getActive() {
        return active;
    }

    public String getVerified_token() {
        return verified_token;
    }
    public void setVerified_token(String verified_token) {
        this.verified_token = verified_token;
    }
}
