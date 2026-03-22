package com.buenws.buenws_backend.API.Entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "user_assets")
public class UserAssetsEntity {

    //Columns
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long assetId;


    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;
}
