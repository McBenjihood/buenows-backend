package com.buenws.buenws_backend.Unit;


import com.buenws.buenws_backend.API.Entity.Inventory.StoreEntity;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.Repositories.Inventory.StoreRepository;
import com.buenws.buenws_backend.API.Service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class InventoryUnitTests {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    public void TestCreateStore(){
        //Arrange
        String name = "Amazing Store";
        String api_key = "S9Wgzx0vk5IEZUPnTgEe2HiEuzoDtvLa";
        Records.CreateStoreRequest record = new Records.CreateStoreRequest(name, api_key);

        //Act
        Records.ApiResponse<Void> response = inventoryService.CreateStore(record);

        //Assert
        assertEquals("Store created successfully", response.message());
        assertNotNull(response);

        ArgumentCaptor<StoreEntity> captor = ArgumentCaptor.forClass(StoreEntity.class);
        verify(storeRepository).save(captor.capture());
        StoreEntity savedEntity = captor.getValue();

        assertEquals(name, savedEntity.getName());
        assertEquals(api_key, savedEntity.getApi_key());
    }

}
