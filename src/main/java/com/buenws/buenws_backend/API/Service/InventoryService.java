package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.Inventory.StoreEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidInventoryOperationException;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.Repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final UserRepository userRepository;

    public InventoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Records.ApiResponse<Void> CreateStore(Records.CreateStoreRequest createStoreRequest, UserEntity userEntity){
        if (userEntity.getStoreEntity() == null){
            StoreEntity storeEntity = new StoreEntity();
            storeEntity.setName(createStoreRequest.name());
            storeEntity.setApi_key(createStoreRequest.api_key());

            userEntity.setStoreEntity(storeEntity);
            storeEntity.setUserEntity(userEntity);
            userRepository.save(userEntity);

            return Records.ApiResponse.success("Store created successfully");
        }else {
            throw new InvalidInventoryOperationException("This User already has a store","INVALID_STORE_OPERATION");
        }
    }

    public void CreateProduct(){

    }
}
