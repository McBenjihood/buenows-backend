package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.InquiryService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.buenws.buenws_backend.Util.RequestUtil;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/inquiry")
public class InquiryController {
    private final InquiryService inquiryService;

    private final Cache<String, Bucket> buckets = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    private Bucket getBucket(String key) {
        try {
            return buckets.get(key, () -> Bucket.builder()
                    .addLimit(Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))))
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    //Contact Form Submission
    @PostMapping("/contact-submissions")
    public ResponseEntity<Records.ApiResponse<Void>> submitContactForm(
            @Valid @RequestBody Records.FormSubmissionRequest formSubmissionRequest,
            HttpServletRequest request
    ) {
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("contact-submissions:" + ipAddress).tryConsume(10)) {
            return ResponseEntity.ok(inquiryService.submitContactForm(formSubmissionRequest));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
}
