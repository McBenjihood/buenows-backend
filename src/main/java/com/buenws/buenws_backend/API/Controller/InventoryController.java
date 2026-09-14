package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.RateLimitService;
import com.buenws.buenws_backend.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final RateLimitService rateLimitService;

    public InventoryController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/products/create")
    public ResponseEntity<Records.ApiResponse<Void>> createProduct(HttpServletRequest request){
        rateLimitService.checkBucket("/products/create:" + RequestUtil.getClientIp(request), 10);
        return ResponseEntity.ok(Records.ApiResponse.success("Wip"));
    }
}
