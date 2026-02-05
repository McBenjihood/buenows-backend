package com.buenws.buenws_backend.api.entity;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private UUID id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_authorities", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "authority")
    private List<String> authorities;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private RefreshTokenEntity refreshTokenEntity;


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

    public RefreshTokenEntity getRefreshTokenEntity() {
        return refreshTokenEntity;
    }
    public void setRefreshTokenEntity(RefreshTokenEntity refreshTokenEntity) {
        this.refreshTokenEntity = refreshTokenEntity;
    }
}
