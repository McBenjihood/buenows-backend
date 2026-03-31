package com.buenws.buenws_backend.API.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_assets")
public class UserAssetEntity {

    public UserAssetEntity() {

    }
    public UserAssetEntity(Long assetId, String type, String url, String path, UserEntity user) {
        this.assetId = assetId;
        this.type = type;
        this.url = url;
        this.path = path;
        this.user = user;
    }

    //Columns
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "path", nullable = false, columnDefinition = "TEXT")
    private String path;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime created_at;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;


    //Getters & Setters
    public Long getAssetId() {
        return assetId;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    public OffsetDateTime getCreated_at() {
        return created_at;
    }

    public UserEntity getUser() {
        return user;
    }
}
