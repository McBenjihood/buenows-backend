package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.RateLimitService;
import com.buenws.buenws_backend.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final RateLimitService rateLimitService;

    public DebugController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/ip")
    public Map<String, String> debugIP(HttpServletRequest request) {
        return Map.of(
                "remoteAddr", request.getRemoteAddr(),
                "xForwardedFor", Optional.ofNullable(request.getHeader("X-Forwarded-For")).orElse(""),
                "xForwardedProto", Optional.ofNullable(request.getHeader("X-Forwarded-Proto")).orElse(""),
                "xForwardedPort", Optional.ofNullable(request.getHeader("X-Forwarded-Port")).orElse("")
        );
    }
}
