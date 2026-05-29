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
    public record ChatResponse(String reply, String sessionId, String language, boolean sessionEnded, HandoffDraft handoffDraft) {
        public ChatResponse(String reply, String sessionId, String language, boolean sessionEnded) {
            this(reply, sessionId, language, sessionEnded, null);
        }
    }
    public record HandoffDraft(String email, String title, String message, String contactUrl) {}
    public record HealthResponse(String status, String service, String time, Map<String, Object> details) {}
    public record ErrorResponse(String error, String code, Integer retryAfter) {
        public ErrorResponse(String error) { this(error, null, null); }
    }
    public enum ClassificationDecision { ANSWER, CLARIFY, REJECT }
    public record Classification(ClassificationDecision decision, String category, double confidence, String reason) {}
    public record AssistantMetadata(String reply, boolean readyForHandoff, String contactEmail,
                                    String desiredSolution, String leadSummary, String nextAction,
                                    String missingField, boolean projectContextComplete, boolean contactRequired) {
        public AssistantMetadata(String reply, boolean readyForHandoff, String contactEmail,
                                 String desiredSolution, String leadSummary) {
            this(reply, readyForHandoff, contactEmail, desiredSolution, leadSummary, "", "", false, false);
        }

        public AssistantMetadata(String reply, boolean readyForHandoff, String contactEmail, String ignoredContactPhone,
                                 String desiredSolution, String leadSummary) {
            this(reply, readyForHandoff, contactEmail, desiredSolution, leadSummary, "", "", false, false);
        }

        public AssistantMetadata(String reply, boolean readyForHandoff, String contactEmail, String ignoredContactPhone,
                                 String desiredSolution, String leadSummary, String nextAction,
                                 String missingField, boolean projectContextComplete, boolean contactRequired) {
            this(reply, readyForHandoff, contactEmail, desiredSolution, leadSummary, nextAction, missingField, projectContextComplete, contactRequired);
        }

        public static AssistantMetadata empty(String reply) {
            return new AssistantMetadata(reply, false, "", "", "", "", "", false, false);
        }
    }
    public record ContactInfo(String email, String phone) {
        public boolean hasEmail() { return !email.isBlank(); }
    }
    public record ConversationMessage(String role, String content) {}

    public static class ChatSession {
        private final String id;
        private final String language;
        private final Instant createdAt;
        private Instant updatedAt;
        private int userMessageCount;
        private final List<ConversationMessage> messages = new ArrayList<>();
        private boolean ended;
        private String endedReason = "";

        public ChatSession(String language) {
            this(UUID.randomUUID().toString(), language, Instant.now(), Instant.now(), 0, List.of(), false, "");
        }

        public ChatSession(String id, String language, Instant createdAt, Instant updatedAt, int userMessageCount,
                           List<ConversationMessage> messages, boolean ended, String endedReason) {
            this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
            this.language = ChatbotText.resolveLanguage(language);
            this.createdAt = createdAt == null ? Instant.now() : createdAt;
            this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
            this.userMessageCount = Math.max(0, userMessageCount);
            if (messages != null) this.messages.addAll(messages);
            this.ended = ended;
            this.endedReason = endedReason == null ? "" : endedReason;
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
