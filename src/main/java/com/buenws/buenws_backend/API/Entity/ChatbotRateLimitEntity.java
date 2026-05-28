package com.buenws.buenws_backend.API.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "chatbot_rate_limits")
public class ChatbotRateLimitEntity {
    @Id
    @Column(name = "limit_key", nullable = false, length = 255)
    private String limitKey;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Column(name = "reset_at", nullable = false)
    private Instant resetAt;

    public ChatbotRateLimitEntity() {}

    public ChatbotRateLimitEntity(String limitKey, int requestCount, Instant resetAt) {
        this.limitKey = limitKey;
        this.requestCount = requestCount;
        this.resetAt = resetAt;
    }

    public String getLimitKey() {
        return limitKey;
    }

    public void setLimitKey(String limitKey) {
        this.limitKey = limitKey;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    public Instant getResetAt() {
        return resetAt;
    }

    public void setResetAt(Instant resetAt) {
        this.resetAt = resetAt;
    }
}
