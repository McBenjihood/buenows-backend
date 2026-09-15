package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.RepositoryRetrieval;
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
    private final RepositoryRetrieval repositoryRetrieval;

    public InventoryController(RateLimitService rateLimitService, InventoryService inventoryService, RepositoryRetrieval repositoryRetrieval) {
        this.rateLimitService = rateLimitService;
        this.inventoryService = inventoryService;
        this.repositoryRetrieval = repositoryRetrieval;
    }

    @PostMapping("/store/create")
    public ResponseEntity<Records.ApiResponse<Void>> createStore(@Valid @RequestBody Records.CreateStoreRequest requestBody, HttpServletRequest request){
        rateLimitService.checkBucket("/products/create:" + RequestUtil.getClientIp(request), 10);
        return ResponseEntity.ok(inventoryService.CreateStore(requestBody, repositoryRetrieval.getUserEntityFromRequest(request)));
    }
}
