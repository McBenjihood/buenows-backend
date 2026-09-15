package com.buenws.buenws_backend;

import com.buenws.buenws_backend.Integration.InventoryIntegrationTests;
import com.buenws.buenws_backend.Unit.InventoryUnitTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;


import static org.mockito.Mockito.verify;

@Suite
@SelectClasses({
        InventoryUnitTests.class,
        InventoryIntegrationTests.class
})
class BuenwsBackendApplicationTests {

}