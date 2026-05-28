package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatbotSessionStore {
    private final ChatbotProperties properties;
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    public ChatbotSessionStore(ChatbotProperties properties) {
        this.properties = properties;
    }

    public ChatSession create(String language) {
        ChatSession session = new ChatSession(language);
        sessions.put(session.id(), session);
        return session;
    }

    public void put(ChatSession session) {
        if (session != null && isValidSessionId(session.id()) && !isExpired(session)) {
            sessions.put(session.id(), session);
        }
    }

    public ChatSession find(String sessionId) {
        if (!isValidSessionId(sessionId)) return null;
        ChatSession session = sessions.get(sessionId);
        if (session == null) return null;
        if (isExpired(session)) {
            sessions.remove(session.id());
            return null;
        }
        return session;
    }

    public void delete(String sessionId) {
        if (sessionId != null) sessions.remove(sessionId);
    }

    public int activeSessions() {
        cleanupExpiredSessions();
        return sessions.size();
    }

    public boolean hasReachedSessionMessageLimit(ChatSession session) {
        return properties.rateLimitsActive() && session.userMessageCount() >= properties.sessionMessageLimit();
    }

    public int sessionRetryAfterSeconds(ChatSession session) {
        long seconds = Duration.between(Instant.now(), session.createdAt().plus(Duration.ofMinutes(properties.sessionTtlMinutes()))).toSeconds();
        return Math.max(1, (int) seconds);
    }

    @Scheduled(fixedDelayString = "PT15M")
    public void cleanupExpiredSessions() {
        for (ChatSession session : sessions.values()) {
            if (isExpired(session)) sessions.remove(session.id());
        }
    }

    private boolean isExpired(ChatSession session) {
        return Instant.now().isAfter(session.updatedAt().plus(Duration.ofMinutes(properties.sessionTtlMinutes())));
    }

    private boolean isValidSessionId(String sessionId) {
        try {
            UUID.fromString(sessionId == null ? "" : sessionId.trim());
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
