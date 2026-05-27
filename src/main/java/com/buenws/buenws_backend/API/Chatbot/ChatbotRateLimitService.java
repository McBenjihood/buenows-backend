package com.buenws.buenws_backend.API.Chatbot;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatbotRateLimitService {
    private final ChatbotProperties properties;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public ChatbotRateLimitService(ChatbotProperties properties) {
        this.properties = properties;
    }

    public void checkChatLimits(String clientKey, String language) {
        if (!properties.rateLimitsActive()) return;
        check("chat:minute:" + clientKey, properties.chatRateLimitPerMinute(), Duration.ofMinutes(1), "chat_rate_limit_minute", ChatbotText.t(language, "tooManyMessages"));
        check("chat:day:" + clientKey, properties.chatRateLimitPerDay(), Duration.ofDays(1), "chat_rate_limit_day", ChatbotText.t(language, "dailyChatLimit"));
    }

    public void checkSessionLimits(String clientKey, String language) {
        if (!properties.rateLimitsActive()) return;
        check("session:minute:" + clientKey, properties.sessionRateLimitPerMinute(), Duration.ofMinutes(1), "session_rate_limit_minute", ChatbotText.t(language, "tooManySessions"));
        check("session:day:" + clientKey, properties.sessionRateLimitPerDay(), Duration.ofDays(1), "session_rate_limit_day", ChatbotText.t(language, "dailySessionLimit"));
    }

    private void check(String key, int limit, Duration window, String code, String error) {
        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            Instant now = Instant.now();
            if (existing == null || !now.isBefore(existing.resetAt)) return new WindowCounter(now.plus(window), 1);
            existing.count += 1;
            return existing;
        });
        if (counter.count > limit) {
            long retryAfter = Duration.between(Instant.now(), counter.resetAt).toSeconds();
            throw new ChatbotRateLimitExceededException(code, error, Math.max(1, (int) retryAfter));
        }
    }

    private static class WindowCounter {
        private final Instant resetAt;
        private int count;
        private WindowCounter(Instant resetAt, int count) {
            this.resetAt = resetAt;
            this.count = count;
        }
    }
}
