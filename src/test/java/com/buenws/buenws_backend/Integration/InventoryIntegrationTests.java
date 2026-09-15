package com.buenws.buenws_backend.Integration;

import com.buenws.buenws_backend.API.Repository.Repositories.Inventory.StoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class InventoryIntegrationTests {

    @Autowired
    StoreRepository storeRepository;

    @AfterEach
    public void tearDown() {
        storeRepository.deleteAll();
    }

    @Test
    public void TestOneStorePerAccount() {
        // Arrange

        // Act
        // Assert
    }
}
