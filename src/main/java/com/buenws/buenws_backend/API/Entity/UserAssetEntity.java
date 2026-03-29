package com.buenws.buenws_backend.API.Entity;

import jakarta.persistence.*;

import java.nio.file.Path;
import java.util.UUID;

@Entity
@Table(name = "user_assets")
public class UserAssetEntity {

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

    public UserEntity getUser() {
        return user;
    }
}
