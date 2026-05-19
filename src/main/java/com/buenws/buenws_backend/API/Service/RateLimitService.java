package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Exception.Custom.RateLimitException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private final Cache<String, Bucket> buckets = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    private Bucket getBucket(String key) {
        try {
            return buckets.get(key, () -> Bucket.builder()
                    .addLimit(Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))))
                    .build());
        } catch (Exception e) {
            throw new RateLimitException("Please wait a moment. You tried to do this to often.","RATELIMIT_REACHED",e);
        }
    }

    public void checkBucket(String bucket_identifier, Integer consume_tokens) {
        if(!getBucket(bucket_identifier).tryConsume(consume_tokens)){
            throw new RateLimitException("Please wait a moment. You tried to do this to often.","RATELIMIT_REACHED");
        }
    }
}
