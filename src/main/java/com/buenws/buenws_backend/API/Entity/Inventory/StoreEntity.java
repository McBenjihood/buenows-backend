package com.buenws.buenws_backend.API.Entity.Inventory;

import com.buenws.buenws_backend.API.Entity.UserEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stores")
public class StoreEntity {

    public StoreEntity(){
        this.storeID = UUID.randomUUID();
    }

    @Id
    @Column(name = "store_id")
    private UUID storeID;

    @Column(name = "name")
    private String name;

    @Column(name = "api_key")
    private String api_key;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private UserEntity userEntity;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductEntity> productEntityList;


    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getApi_key() {
        return api_key;
    }
    public void setApi_key(String api_key) {
        this.api_key = api_key;
    }

    public List<ProductEntity> getProductEntityList() {
        return productEntityList;
    }
    public void setProductEntityList(List<ProductEntity> productEntityList) {
        this.productEntityList = productEntityList;
    }

    public UserEntity getUserEntity() {
        return userEntity;
    }
    public void setUserEntity(UserEntity userEntity) {
        this.userEntity = userEntity;
    }
}
