package com.buenws.buenws_backend.API.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    //Constructors
    public UserEntity(){}
    public UserEntity(String email){
        this.email = email;
    }
    public UserEntity(String email, List<String> authorities) {
        this.authorities = authorities;
        this.email = email;
    }

    //Columns
    @Id
    @GeneratedValue
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_authorities", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "authority")
    private List<String> authorities;

    @Column(name = "email")
    private String email;

    @Column(name = "first_name")
    private String first_name;

    @Column(name = "last_name")
    private String last_name;

    @Column(name = "password")
    private String password;
    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime created_at;

    @OneToOne(mappedBy = "userEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private RefreshTokenEntity refreshTokenEntity;

    @OneToOne(mappedBy = "userEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private ResetCodeEntity resetCodeEntity;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserAssetEntity> userAssets;


    // Getters / Setters
    public UUID getId() {
        return id;
    }

    public List<String> getAuthorities() {
        return authorities;
    }
    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public String getPassword(){return password;}

    public OffsetDateTime getCreated_at() {
        return created_at;
    }

    public String getFirst_name() {
        return first_name;
    }
    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }
    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    // Getters / Setters for Linked Tables
    public RefreshTokenEntity getRefreshTokenEntity() {
        return refreshTokenEntity;
    }
    public void setRefreshTokenEntity(RefreshTokenEntity refreshTokenEntity) {
        this.refreshTokenEntity = refreshTokenEntity;
    }

    public List<UserAssetEntity> getUserAssets() {
        return userAssets;
    }
    public void setUserAssets(List<UserAssetEntity> userAssets) {
        this.userAssets = userAssets;
    }

    public ResetCodeEntity getResetCodeEntity() {
        return resetCodeEntity;
    }
    public void setResetCodeEntity(ResetCodeEntity resetCodeEntity) {
        this.resetCodeEntity = resetCodeEntity;
    }
}
