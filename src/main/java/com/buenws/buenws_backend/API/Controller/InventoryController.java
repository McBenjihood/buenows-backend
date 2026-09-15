package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.InventoryService;
import com.buenws.buenws_backend.API.Service.RateLimitService;
import com.buenws.buenws_backend.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final RateLimitService rateLimitService;
    private final InventoryService inventoryService;

    public InventoryController(RateLimitService rateLimitService, InventoryService inventoryService) {
        this.rateLimitService = rateLimitService;
        this.inventoryService = inventoryService;
    }

    @PostMapping("/products/create")
    public ResponseEntity<Records.ApiResponse<Void>> createProduct(@Valid @RequestBody Records.CreateStoreRequest requestbody HttpServletRequest request){
        rateLimitService.checkBucket("/products/create:" + RequestUtil.getClientIp(request), 10);
        return ResponseEntity.ok(inventoryService.CreateStore(requestbody, requestww));
    }
}
