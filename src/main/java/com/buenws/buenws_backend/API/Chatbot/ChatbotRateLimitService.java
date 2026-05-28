package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Entity.ChatbotRateLimitEntity;
import com.buenws.buenws_backend.API.Repository.Repositories.ChatbotRateLimitRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;

@Service
public class ChatbotRateLimitService {
    private final ChatbotProperties properties;
    private final ChatbotRateLimitRepository rateLimitRepository;

    public ChatbotRateLimitService(ChatbotProperties properties, ChatbotRateLimitRepository rateLimitRepository) {
        this.properties = properties;
        this.rateLimitRepository = rateLimitRepository;
    }

    @Transactional
    public void checkChatLimits(String clientKey, String language) {
        if (!properties.rateLimitsActive()) return;
        check("chat:minute:" + clientKey, properties.chatRateLimitPerMinute(), Duration.ofMinutes(1), "chat_rate_limit_minute", ChatbotText.t(language, "tooManyMessages"));
        check("chat:day:" + clientKey, properties.chatRateLimitPerDay(), Duration.ofDays(1), "chat_rate_limit_day", ChatbotText.t(language, "dailyChatLimit"));
    }

    @Transactional
    public void checkSessionLimits(String clientKey, String language) {
        if (!properties.rateLimitsActive()) return;
        check("session:minute:" + clientKey, properties.sessionRateLimitPerMinute(), Duration.ofMinutes(1), "session_rate_limit_minute", ChatbotText.t(language, "tooManySessions"));
        check("session:day:" + clientKey, properties.sessionRateLimitPerDay(), Duration.ofDays(1), "session_rate_limit_day", ChatbotText.t(language, "dailySessionLimit"));
    }

    private void check(String key, int limit, Duration window, String code, String error) {
        Instant now = Instant.now();
        rateLimitRepository.insertIfAbsent(key, now.plus(window));
        ChatbotRateLimitEntity counter = rateLimitRepository.findByLimitKeyForUpdate(key)
                .orElseGet(() -> new ChatbotRateLimitEntity(key, 0, now.plus(window)));

        if (!now.isBefore(counter.getResetAt())) {
            counter.setRequestCount(0);
            counter.setResetAt(now.plus(window));
        }

        counter.setRequestCount(counter.getRequestCount() + 1);
        rateLimitRepository.save(counter);

        if (counter.getRequestCount() > limit) {
            long retryAfter = Duration.between(now, counter.getResetAt()).toSeconds();
            throw new ChatbotRateLimitExceededException(code, error, Math.max(1, (int) retryAfter));
        }
    }

    @Scheduled(fixedDelayString = "${app.chatbot.rate-limit-cleanup-interval:PT30M}")
    @Transactional
    public void cleanupExpiredCounters() {
        rateLimitRepository.deleteByResetAtBefore(Instant.now());
    }
}
