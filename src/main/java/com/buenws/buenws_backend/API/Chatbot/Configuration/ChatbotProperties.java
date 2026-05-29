package com.buenws.buenws_backend.API.Chatbot.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ChatbotProperties {
    private final String openAiApiKey;
    private final String openAiModel;
    private final String openAiGuardModel;
    private final String openAiReasoningEffort;
    private final int maxMessageLength;
    private final int maxOutputTokens;
    private final int maxGuardOutputTokens;
    private final int maxContextMessages;
    private final int maxContextChars;
    private final int sessionTtlMinutes;
    private final int historyRetentionDays;
    private final String trustedProxyCidrs;
    private final boolean healthDetails;
    private final boolean rateLimitsActive;
    private final int chatRateLimitPerMinute;
    private final int chatRateLimitPerDay;
    private final int sessionRateLimitPerMinute;
    private final int sessionRateLimitPerDay;
    private final int sessionMessageLimit;
    private final Path companyConfigPath;
    private final String frontendContactUrl;

    public ChatbotProperties(
            @Value("${app.chatbot.openai-api-key:${OPENAI_API_KEY:}}") String openAiApiKey,
            @Value("${app.chatbot.openai-model:${OPENAI_MODEL:gpt-4o-mini}}") String openAiModel,
            @Value("${app.chatbot.openai-guard-model:${OPENAI_GUARD_MODEL:${OPENAI_MODEL:gpt-4o-mini}}}") String openAiGuardModel,
            @Value("${app.chatbot.openai-reasoning-effort:${OPENAI_REASONING_EFFORT:low}}") String openAiReasoningEffort,
            @Value("${app.chatbot.max-message-length:${MAX_MESSAGE_LENGTH:800}}") int maxMessageLength,
            @Value("${app.chatbot.max-output-tokens:${MAX_OUTPUT_TOKENS:500}}") int maxOutputTokens,
            @Value("${app.chatbot.max-guard-output-tokens:${MAX_GUARD_OUTPUT_TOKENS:320}}") int maxGuardOutputTokens,
            @Value("${app.chatbot.max-context-messages:${MAX_CONTEXT_MESSAGES:16}}") int maxContextMessages,
            @Value("${app.chatbot.max-context-chars:${MAX_CONTEXT_CHARS:9000}}") int maxContextChars,
            @Value("${app.chatbot.session-ttl-minutes:${SESSION_TTL_MINUTES:360}}") int sessionTtlMinutes,
            @Value("${app.chatbot.history-retention-days:${CHATBOT_HISTORY_RETENTION_DAYS:7}}") int historyRetentionDays,
            @Value("${app.trusted-proxy-cidrs:${APP_TRUSTED_PROXY_CIDRS:loopback,private}}") String trustedProxyCidrs,
            @Value("${app.chatbot.health-details:${CHATBOT_HEALTH_DETAILS:false}}") boolean healthDetails,
            @Value("${app.chatbot.rate-limits:${RATE_LIMITS:active}}") String rateLimits,
            @Value("${app.chatbot.chat-rate-limit-per-minute:${CHAT_RATE_LIMIT_PER_MINUTE:10}}") int chatRateLimitPerMinute,
            @Value("${app.chatbot.chat-rate-limit-per-day:${CHAT_RATE_LIMIT_PER_DAY:60}}") int chatRateLimitPerDay,
            @Value("${app.chatbot.session-rate-limit-per-minute:${SESSION_RATE_LIMIT_PER_MINUTE:10}}") int sessionRateLimitPerMinute,
            @Value("${app.chatbot.session-rate-limit-per-day:${SESSION_RATE_LIMIT_PER_DAY:10}}") int sessionRateLimitPerDay,
            @Value("${app.chatbot.session-message-limit:${SESSION_MESSAGE_LIMIT:60}}") int sessionMessageLimit,
            @Value("${app.chatbot.company-config-path:${COMPANY_CONFIG_PATH:}}") String companyConfigPath,
            @Value("${app.frontend.contact-url:${APP_FRONTEND_CONTACT_URL:http://localhost:5173/contact}}") String frontendContactUrl
    ) {
        this.openAiApiKey = clean(openAiApiKey);
        this.openAiModel = withDefault(openAiModel, "gpt-4o-mini");
        this.openAiGuardModel = withDefault(openAiGuardModel, this.openAiModel);
        this.openAiReasoningEffort = normalizeReasoningEffort(openAiReasoningEffort);
        this.maxMessageLength = positive(maxMessageLength, 800);
        this.maxOutputTokens = positive(maxOutputTokens, 500);
        this.maxGuardOutputTokens = positive(maxGuardOutputTokens, 320);
        this.maxContextMessages = positive(maxContextMessages, 16);
        this.maxContextChars = positive(maxContextChars, 9000);
        this.sessionTtlMinutes = positive(sessionTtlMinutes, 360);
        this.historyRetentionDays = Math.min(7, positive(historyRetentionDays, 7));
        this.trustedProxyCidrs = withDefault(trustedProxyCidrs, "loopback,private");
        this.healthDetails = healthDetails;
        this.rateLimitsActive = !"inactive".equalsIgnoreCase(clean(rateLimits));
        this.chatRateLimitPerMinute = positive(chatRateLimitPerMinute, 10);
        this.chatRateLimitPerDay = positive(chatRateLimitPerDay, 60);
        this.sessionRateLimitPerMinute = positive(sessionRateLimitPerMinute, 10);
        this.sessionRateLimitPerDay = positive(sessionRateLimitPerDay, 10);
        this.sessionMessageLimit = positive(sessionMessageLimit, 60);
        this.companyConfigPath = clean(companyConfigPath).isBlank()
                ? null
                : Path.of(clean(companyConfigPath)).toAbsolutePath().normalize();
        this.frontendContactUrl = withDefault(frontendContactUrl, "http://localhost:5173/contact");
    }

    public String openAiApiKey() { return openAiApiKey; }
    public String openAiModel() { return openAiModel; }
    public String openAiGuardModel() { return openAiGuardModel; }
    public String openAiReasoningEffort() { return openAiReasoningEffort; }
    public int maxMessageLength() { return maxMessageLength; }
    public int maxOutputTokens() { return maxOutputTokens; }
    public int maxGuardOutputTokens() { return maxGuardOutputTokens; }
    public int maxContextMessages() { return maxContextMessages; }
    public int maxContextChars() { return maxContextChars; }
    public int sessionTtlMinutes() { return sessionTtlMinutes; }
    public int historyRetentionDays() { return historyRetentionDays; }
    public String trustedProxyCidrs() { return trustedProxyCidrs; }
    public boolean healthDetails() { return healthDetails; }
    public boolean rateLimitsActive() { return rateLimitsActive; }
    public int chatRateLimitPerMinute() { return chatRateLimitPerMinute; }
    public int chatRateLimitPerDay() { return chatRateLimitPerDay; }
    public int sessionRateLimitPerMinute() { return sessionRateLimitPerMinute; }
    public int sessionRateLimitPerDay() { return sessionRateLimitPerDay; }
    public int sessionMessageLimit() { return sessionMessageLimit; }
    public Path companyConfigPath() { return companyConfigPath; }
    public String frontendContactUrl() { return frontendContactUrl; }
    public boolean openAiConfigured() {
        String normalized = openAiApiKey.toLowerCase();
        return !openAiApiKey.isBlank()
                && openAiApiKey.startsWith("sk-")
                && !normalized.contains("replace_with")
                && !normalized.contains("placeholder");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String withDefault(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private static int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static String normalizeReasoningEffort(String value) {
        String cleaned = clean(value).toLowerCase();
        return switch (cleaned) {
            case "none", "minimal", "low", "medium", "high", "xhigh" -> cleaned;
            default -> "low";
        };
    }
}
