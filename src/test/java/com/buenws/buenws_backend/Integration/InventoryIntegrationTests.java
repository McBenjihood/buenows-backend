package com.buenws.buenws_backend.Integration;

import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidInventoryOperationException;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.Repositories.Inventory.StoreRepository;
import com.buenws.buenws_backend.API.Service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
public class InventoryIntegrationTests {

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    InventoryService inventoryService;

    @AfterEach
    public void tearDown() {
        storeRepository.deleteAll();
    }

    @Test
    public void TestOneStorePerAccount() {
        // Arrange
        String name = "Amazing Store";
        String api_key = "S9Wgzx0vk5IEZUPnTgEe2HiEuzoDtvLa";

        UserEntity userEntity = new UserEntity();
        Records.CreateStoreRequest record = new Records.CreateStoreRequest(name, api_key);

        // Act
        inventoryService.CreateStore(record, userEntity);

        // Assert
        Exception exception = assertThrows(InvalidInventoryOperationException.class, () -> {
            inventoryService.CreateStore(record, userEntity);
        });
    }
}
