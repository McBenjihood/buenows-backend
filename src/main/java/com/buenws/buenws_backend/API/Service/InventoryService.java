package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.Inventory.StoreEntity;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.Repositories.Inventory.StoreRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final StoreRepository storeRepository;

    public InventoryService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public void CreateStore(Records.CreateStoreRequest createStoreRequest){
        StoreEntity storeEntity = new StoreEntity();
        storeEntity.setName(createStoreRequest.name());
        storeEntity.setApi_key(createStoreRequest.api_key());
        storeRepository.save(storeEntity);
    }

    public void CreateProduct(){

    }
}
