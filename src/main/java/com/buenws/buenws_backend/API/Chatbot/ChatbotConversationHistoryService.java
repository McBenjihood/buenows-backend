package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatSession;
import com.buenws.buenws_backend.API.Entity.ChatbotConversationEntity;
import com.buenws.buenws_backend.API.Entity.ChatbotMessageEntity;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidInquiryException;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.Repositories.ChatbotConversationRepository;
import com.buenws.buenws_backend.API.Repository.Repositories.ChatbotMessageRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ChatbotConversationHistoryService {
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_ERROR = "error";

    private final ChatbotProperties properties;
    private final ChatbotCompanyConfigService companyConfigService;
    private final ChatbotSessionStore sessionStore;
    private final ChatbotConversationRepository conversationRepository;
    private final ChatbotMessageRepository messageRepository;

    public ChatbotConversationHistoryService(ChatbotProperties properties,
                                             ChatbotCompanyConfigService companyConfigService,
                                             ChatbotSessionStore sessionStore,
                                             ChatbotConversationRepository conversationRepository,
                                             ChatbotMessageRepository messageRepository) {
        this.properties = properties;
        this.companyConfigService = companyConfigService;
        this.sessionStore = sessionStore;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public void saveUserMessage(ChatbotCompanyConfig config, ChatSession session, String content) {
        saveMessage(config, session, "user", content);
    }

    @Transactional
    public void saveAssistantMessage(ChatbotCompanyConfig config, ChatSession session, String content, String status) {
        ChatbotConversationEntity conversation = saveMessage(config, session, "assistant", content);
        updateStatus(conversation, status);
    }

    @Transactional
    public void markError(String sessionId) {
        findBySessionId(sessionId).ifPresent(conversation -> updateStatus(conversation, STATUS_ERROR));
    }

    @Transactional(readOnly = true)
    public Records.ApiResponse<List<Records.AdminChatbotCompanyResponse>> getCompanies() {
        ChatbotCompanyConfig config = companyConfigService.loadConfig();
        return Records.ApiResponse.success("Chatbot companies loaded successfully.", List.of(
                new Records.AdminChatbotCompanyResponse(config.companyKey(), config.companyName())
        ));
    }

    @Transactional(readOnly = true)
    public Records.ApiResponse<List<Records.AdminChatbotConversationSummaryResponse>> getConversationSummaries(String requestedCompanyKey) {
        ChatbotCompanyConfig config = companyConfigService.loadConfig();
        String companyKey = normalizeCompanyKey(requestedCompanyKey, config.companyKey());
        Instant cutoff = Instant.now().minus(properties.historyRetentionDays(), ChronoUnit.DAYS);

        List<Records.AdminChatbotConversationSummaryResponse> conversations = conversationRepository
                .findByCompanyKeyAndCreatedAtAfterOrderByCreatedAtDesc(companyKey, cutoff)
                .stream()
                .map(this::toSummary)
                .toList();

        return Records.ApiResponse.success("Chatbot conversations loaded successfully.", conversations);
    }

    @Transactional(readOnly = true)
    public Records.ApiResponse<Records.AdminChatbotConversationDetailResponse> getConversationDetail(UUID conversationId) {
        ChatbotConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new InvalidInquiryException("Chatbot conversation not found.", "INVALID_INQUIRY"));
        List<Records.AdminChatbotMessageResponse> messages = messageRepository
                .findByConversationConversationIdOrderBySequenceNumberAsc(conversationId)
                .stream()
                .map(this::toMessage)
                .toList();

        return Records.ApiResponse.success("Chatbot conversation loaded successfully.", toDetail(conversation, messages));
    }

    @Transactional
    public Records.ApiResponse<Void> deleteConversation(UUID conversationId) {
        ChatbotConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new InvalidInquiryException("Chatbot conversation not found.", "INVALID_INQUIRY"));
        sessionStore.delete(conversation.getSessionId().toString());
        conversationRepository.delete(conversation);
        return Records.ApiResponse.success("Chatbot conversation deleted successfully.");
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.chatbot.history-cleanup-interval:PT1H}")
    public void deleteExpiredConversations() {
        conversationRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private ChatbotConversationEntity saveMessage(ChatbotCompanyConfig config, ChatSession session, String role, String content) {
        ChatbotConversationEntity conversation = ensureConversation(config, session);
        ChatbotMessageEntity message = new ChatbotMessageEntity();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(ChatbotText.cleanText(content, 4000));
        message.setSequenceNumber(conversation.nextSequenceNumber());
        messageRepository.save(message);

        if ("user".equals(role) && (conversation.getPreview() == null || conversation.getPreview().isBlank())) {
            conversation.setPreview(ChatbotText.cleanInlineText(content, 220));
        }
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
        return conversation;
    }

    private ChatbotConversationEntity ensureConversation(ChatbotCompanyConfig config, ChatSession session) {
        UUID sessionId = UUID.fromString(session.id());
        return conversationRepository.findBySessionId(sessionId).orElseGet(() -> {
            Instant now = Instant.now();
            ChatbotConversationEntity conversation = new ChatbotConversationEntity();
            conversation.setSessionId(sessionId);
            conversation.setCompanyKey(config.companyKey());
            conversation.setCompanyName(config.companyName());
            conversation.setLanguage(session.language());
            conversation.setStatus(STATUS_ACTIVE);
            conversation.setCreatedAt(now);
            conversation.setUpdatedAt(now);
            conversation.setExpiresAt(now.plus(properties.historyRetentionDays(), ChronoUnit.DAYS));
            return conversationRepository.save(conversation);
        });
    }

    private java.util.Optional<ChatbotConversationEntity> findBySessionId(String sessionId) {
        try {
            return conversationRepository.findBySessionId(UUID.fromString(sessionId));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private void updateStatus(ChatbotConversationEntity conversation, String status) {
        if (status == null || status.isBlank()) status = STATUS_ACTIVE;
        conversation.setStatus(status);
        conversation.setUpdatedAt(Instant.now());
        if (STATUS_COMPLETED.equals(status) || STATUS_REJECTED.equals(status) || STATUS_ERROR.equals(status)) {
            conversation.setEndedAt(Instant.now());
        }
        conversationRepository.save(conversation);
    }

    private String normalizeCompanyKey(String requestedCompanyKey, String fallback) {
        String cleaned = ChatbotText.cleanInlineText(requestedCompanyKey, 120);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private Records.AdminChatbotConversationSummaryResponse toSummary(ChatbotConversationEntity conversation) {
        return new Records.AdminChatbotConversationSummaryResponse(
                conversation.getConversationId().toString(),
                conversation.getCompanyKey(),
                conversation.getCompanyName(),
                conversation.getLanguage(),
                conversation.getStatus(),
                conversation.getMessageCount(),
                conversation.getPreview(),
                toString(conversation.getCreatedAt()),
                toString(conversation.getUpdatedAt()),
                toString(conversation.getExpiresAt())
        );
    }

    private Records.AdminChatbotConversationDetailResponse toDetail(ChatbotConversationEntity conversation, List<Records.AdminChatbotMessageResponse> messages) {
        return new Records.AdminChatbotConversationDetailResponse(
                conversation.getConversationId().toString(),
                conversation.getCompanyKey(),
                conversation.getCompanyName(),
                conversation.getLanguage(),
                conversation.getStatus(),
                conversation.getMessageCount(),
                conversation.getPreview(),
                toString(conversation.getCreatedAt()),
                toString(conversation.getUpdatedAt()),
                toString(conversation.getEndedAt()),
                toString(conversation.getExpiresAt()),
                messages
        );
    }

    private Records.AdminChatbotMessageResponse toMessage(ChatbotMessageEntity message) {
        return new Records.AdminChatbotMessageResponse(
                message.getMessageId() == null ? 0 : message.getMessageId(),
                message.getSequenceNumber(),
                message.getRole(),
                message.getContent(),
                toString(message.getCreatedAt())
        );
    }

    private String toString(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
