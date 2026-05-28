package com.buenws.buenws_backend.API.Chatbot;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChatbotModels {
    private ChatbotModels() {}

    public record ConfigResponse(String botName, String companyName, String subtitle, String welcomeMessage,
                                 String placeholder, String privacyNotice, int maxMessageLength,
                                 Map<String, Object> theme, Map<String, Object> handoff, Map<String, Object> contact) {}
    public record SessionRequest(String language) {}
    public record SessionResponse(String sessionId, String language) {}
    public record ChatRequest(String sessionId, String language, @Size(max = 4000) String message) {}
    public record ChatResponse(String reply, String sessionId, String language, boolean sessionEnded) {}
    public record HealthResponse(String status, String service, String time, Map<String, Object> details) {}
    public record ErrorResponse(String error, String code, Integer retryAfter) {
        public ErrorResponse(String error) { this(error, null, null); }
    }
    public enum ClassificationDecision { ANSWER, CLARIFY, REJECT }
    public record Classification(ClassificationDecision decision, String category, double confidence, String reason) {}
    public record AssistantMetadata(String reply, boolean readyForHandoff, String contactEmail, String contactPhone,
                                    String desiredSolution, String leadSummary) {
        public static AssistantMetadata empty(String reply) {
            return new AssistantMetadata(reply, false, "", "", "", "");
        }
    }
    public record ContactInfo(String email, String phone) {
        public boolean hasAny() { return !email.isBlank() || !phone.isBlank(); }
    }
    public record ConversationMessage(String role, String content) {}

    public static class ChatSession {
        private final String id = UUID.randomUUID().toString();
        private final String language;
        private final Instant createdAt = Instant.now();
        private Instant updatedAt = createdAt;
        private int userMessageCount;
        private final List<ConversationMessage> messages = new ArrayList<>();
        private boolean ended;
        private String endedReason = "";

        public ChatSession(String language) {
            this.language = ChatbotText.resolveLanguage(language);
        }
        public String id() { return id; }
        public String language() { return language; }
        public Instant createdAt() { return createdAt; }
        public Instant updatedAt() { return updatedAt; }
        public int userMessageCount() { return userMessageCount; }
        public List<ConversationMessage> messages() { return messages; }
        public boolean ended() { return ended; }
        public String endedReason() { return endedReason; }
        public void touch() { updatedAt = Instant.now(); }
        public void incrementUserMessageCount() { userMessageCount += 1; touch(); }
        public void end(String reason) { ended = true; endedReason = reason == null ? "" : reason; touch(); }
        public void addMessage(String role, String content, int maxLength) {
            messages.add(new ConversationMessage(role, ChatbotText.cleanText(content, maxLength)));
            touch();
        }
    }
}
