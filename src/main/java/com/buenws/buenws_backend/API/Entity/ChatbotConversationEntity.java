package com.buenws.buenws_backend.API.Entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chatbot_conversations")
public class ChatbotConversationEntity {
    @Id
    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId = UUID.randomUUID();

    @Column(name = "session_id", nullable = false, unique = true, updatable = false)
    private UUID sessionId;

    @Column(name = "company_key", nullable = false, length = 120)
    private String companyKey;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "language", nullable = false, length = 8)
    private String language;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    @Column(name = "preview", columnDefinition = "TEXT")
    private String preview;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = createdAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatbotMessageEntity> messages = new ArrayList<>();

    public UUID getConversationId() { return conversationId; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public String getCompanyKey() { return companyKey; }
    public void setCompanyKey(String companyKey) { this.companyKey = companyKey; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public List<ChatbotMessageEntity> getMessages() { return messages; }

    public int nextSequenceNumber() {
        messageCount += 1;
        updatedAt = Instant.now();
        return messageCount;
    }
}
