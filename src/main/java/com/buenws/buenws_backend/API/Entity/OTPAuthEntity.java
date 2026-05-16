package com.buenws.buenws_backend.API.Entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "otp_auth")
public class OTPAuthEntity {

    public OTPAuthEntity(){}

    public OTPAuthEntity(UserEntity userEntity){
        this.otp_code = "";
        this.verified_token = null;
        this.userEntity = userEntity;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "otp_code")
    private String otp_code;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "active")
    private boolean active;

    @Column(name = "verified_token")
    UUID verified_token;

    @Column(name = "updated_at")
    private Instant updated_at;

    @Column(name = "expires_at")
    private Instant expires_at;

    @Column(name = "cooldown")
    private Instant cooldown;

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
    public String getOtp_code() {
        return otp_code;
    }
    public void setOtp_code(String reset_code) {
        this.otp_code = reset_code;
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

    public Instant getCooldown() {
        return cooldown;
    }
    public void setCooldown(Instant cooldown) {
        this.cooldown = cooldown;
    }

    public UUID getVerified_token() {
        return verified_token;
    }
    public void setVerified_token(UUID verified_token) {
        this.verified_token = verified_token;
    }
}
