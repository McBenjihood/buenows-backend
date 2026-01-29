package com.buenws.buenws_backend.api.entity;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserEntity {

    public UserEntity(){}

    public UserEntity(String email){
        this.email = email;
    }

    public UserEntity(String email, List<String> authorities) {
        this.authorities = authorities;
        this.email = email;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_authorities", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "authority")
    private List<String> authorities;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "refresh_tokens", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "refresh_token")
    private List<String> refreshToken;

    private String email;
    private String password;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
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

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
