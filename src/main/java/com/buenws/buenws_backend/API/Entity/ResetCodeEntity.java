package com.buenws.buenws_backend.API.Entity;

import com.buenws.buenws_backend.Util.TimeUtil;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reset_codes")
public class ResetCodeEntity {

    public ResetCodeEntity(){}

    public ResetCodeEntity(Instant updated_at, Instant expires_at){
        this.updated_at = updated_at;
        this.expires_at = expires_at;
        this.reset_code = "";
        this.attempts = 5;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private String code_id;

    @Column(name = "user_id")
    private UUID user_id;

    @Column(name = "reset_code")
    private String reset_code;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "updated_at")
    private Instant updated_at;

    @Column(name = "expires_at")
    private Instant expires_at;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    //Getters
    public String getCode_id() {
        return code_id;
    }
    public UUID getUser_id() {
        return user_id;
    }
    public UserEntity getUserEntity() {
        return userEntity;
    }

    public boolean isActive() {
        return TimeUtil.getCurrentDate().isBefore(expires_at);
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

}
