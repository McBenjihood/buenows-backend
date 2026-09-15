package com.buenws.buenws_backend.API.Entity.Inventory;

import com.buenws.buenws_backend.API.Entity.UserEntity;
import jakarta.mail.Store;
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
    private UUID storeID;

    @Column
    private String name;

    @Column
    private String api_key;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private UserEntity userEntity;

    @OneToMany(mappedBy = "product_id", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
}
