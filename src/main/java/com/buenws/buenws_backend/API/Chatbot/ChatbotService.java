package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.AssistantMetadata;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatSession;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.Classification;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ClassificationDecision;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ConfigResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ConversationMessage;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.HealthResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.SessionResponse;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {

    private final ChatbotProperties properties;
    private final ChatbotCompanyConfigService companyConfigService;
    private final ChatbotSessionStore sessionStore;
    private final OpenAiResponsesClient openAiClient;
    private final PromptBuilder promptBuilder;
    private final ContactExtractor contactExtractor;
    private final ChatbotConversationHistoryService historyService;
    private final ProjectContextEvaluator projectContext;
    private final ReplyQualityGuard replyQuality;
    private final HandoffRenderer handoffRenderer;
    private final LanguageSafetyGuard languageSafety;

    public ChatbotService(ChatbotProperties properties, ChatbotCompanyConfigService companyConfigService,
                          ChatbotSessionStore sessionStore, OpenAiResponsesClient openAiClient,
                          PromptBuilder promptBuilder, ContactExtractor contactExtractor,
                          ChatbotConversationHistoryService historyService, ProjectContextEvaluator projectContext,
                          ReplyQualityGuard replyQuality, HandoffRenderer handoffRenderer,
                          LanguageSafetyGuard languageSafety) {
        this.properties = properties;
        this.companyConfigService = companyConfigService;
        this.sessionStore = sessionStore;
        this.openAiClient = openAiClient;
        this.promptBuilder = promptBuilder;
        this.contactExtractor = contactExtractor;
        this.historyService = historyService;
        this.projectContext = projectContext;
        this.replyQuality = replyQuality;
        this.handoffRenderer = handoffRenderer;
        this.languageSafety = languageSafety;
    }

    public ConfigResponse publicConfig(String language) { return companyConfigService.publicConfig(language); }
    public SessionResponse createSession(String language) {
        ChatSession session = sessionStore.create(language);
        return new SessionResponse(session.id(), session.language());
    }

    public String resolveLanguageForRequest(String requestedSessionId, String requestedLanguage) {
        ChatSession session = findOrRestoreSession(requestedSessionId);
        return session == null ? ChatbotText.resolveLanguage(requestedLanguage) : session.language();
    }

    public ChatResponse chat(String requestedSessionId, String requestedLanguage, String rawMessage) {
        String requestLanguage = ChatbotText.resolveLanguage(requestedLanguage);
        String userMessage = ChatbotText.normalizeInputText(rawMessage);
        if (userMessage.isBlank()) throw new ChatbotValidationException(ChatbotText.t(requestLanguage, "missingMessage"));
        if (userMessage.length() < 2) throw new ChatbotValidationException(ChatbotText.t(requestLanguage, "shortMessage"));
        if (userMessage.length() > properties.maxMessageLength()) throw new ChatbotValidationException(ChatbotText.t(requestLanguage, "messageTooLong", Map.of("max", properties.maxMessageLength())));

        ChatSession session = findOrRestoreSession(requestedSessionId);
        if (session != null && session.ended()) {
            String previousLanguage = session.language();
            sessionStore.delete(session.id());
            session = sessionStore.create(previousLanguage);
        }
        if (session == null) session = sessionStore.create(requestLanguage);
        String language = session.language();
        if (sessionStore.hasReachedSessionMessageLimit(session)) {
            throw new ChatbotRateLimitExceededException("session_message_limit", ChatbotText.t(language, "sessionMessageLimit"), sessionStore.sessionRetryAfterSeconds(session));
        }
        if (!properties.openAiConfigured()) throw new ChatbotUnavailableException(ChatbotText.t(language, "openAiMissing"), 503);

        ChatbotCompanyConfig config = companyConfigService.loadConfig();
        historyService.saveUserMessage(config, session, userMessage);
        try {
            Classification classification = classify(config, session, userMessage, language);
            session.incrementUserMessageCount();
            session.addMessage("user", userMessage, properties.maxMessageLength());

            if (classification.decision() == ClassificationDecision.REJECT) {
                String reply = ChatbotText.t(language, "rejected", Map.of("companyName", config.companyName()));
                session.addMessage("assistant", reply, 4000);
                session.end(classification.category());
                historyService.saveAssistantMessage(config, session, reply, ChatbotConversationHistoryService.STATUS_REJECTED);
                return new ChatResponse(reply, session.id(), language, true);
            }

            String reply = generateReply(config, session, classification, language);
            session.addMessage("assistant", reply, 4000);
            boolean completed = replyQuality.replyLooksLikeTemplate(reply);
            if (completed) session.end("handoff_completed");
            String status = completed
                    ? ChatbotConversationHistoryService.STATUS_COMPLETED
                    : ChatbotConversationHistoryService.STATUS_ACTIVE;
            historyService.saveAssistantMessage(config, session, reply, status);
            return new ChatResponse(reply, session.id(), language, completed);
        } catch (RuntimeException exception) {
            historyService.markError(session.id());
            throw exception;
        }
    }

    public HealthResponse health() {
        boolean configLoaded;
        try { companyConfigService.loadConfig(); configLoaded = true; } catch (RuntimeException ignored) { configLoaded = false; }
        Map<String, Object> details = properties.healthDetails()
                ? Map.of(
                        "chatMode", properties.openAiConfigured() ? "api" : "unconfigured",
                        "openAiConfigured", properties.openAiConfigured(),
                        "configLoaded", configLoaded,
                        "activeSessions", sessionStore.activeSessions()
                )
                : Map.of();
        return new HealthResponse("ok", "professional-website-chatbot", Instant.now().toString(), details);
    }

    private ChatSession findOrRestoreSession(String requestedSessionId) {
        ChatSession session = sessionStore.find(requestedSessionId);
        if (session != null) return session;
        return historyService.restoreActiveSession(requestedSessionId)
                .map(restored -> {
                    sessionStore.put(restored);
                    return restored;
                })
                .orElse(null);
    }

    private Classification classify(ChatbotCompanyConfig config, ChatSession session, String userMessage, String language) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("language", language);
        payload.put("recentMessages", buildConversationForClassification(session, userMessage));
        payload.put("currentMessage", userMessage);
        payload.put("companyContext", companyConfigService.buildCompanyContext(config));
        return openAiClient.classify(promptBuilder.classificationInstructions(config, language), payload);
    }

    private String generateReply(ChatbotCompanyConfig config, ChatSession session, Classification classification, String language) {
        AssistantMetadata metadata = openAiClient.createReply(promptBuilder.replyInstructions(config, classification, language), buildConversationForReply(session));
        String reply = ChatbotText.cleanReply(metadata.reply());
        if (reply.isBlank()) throw new ChatbotUnavailableException(ChatbotText.t(language, "chatUnavailable"), 500);

        for (int attempt = 1; attempt <= 2; attempt += 1) {
            String issue = replyQuality.getReplyQualityIssue(reply, session, metadata, language);
            if (issue == null) break;
            metadata = merge(metadata, openAiClient.repairReply(promptBuilder.repairInstructions(config, classification, language, issue), buildConversationForReply(session), issue, reply));
            reply = ChatbotText.cleanReply(metadata.reply());
        }
        if (replyQuality.needsProjectFollowUpFallback(reply, session)) reply = replyQuality.buildProjectFollowUpFallback(session, language);
        if (replyQuality.needsContactDelayFallback(reply, session)) reply = replyQuality.buildProjectFollowUpFallback(session, language);
        if (replyQuality.asksForContactOption(reply) && replyQuality.lastUserMessageIsContactPreference(session)) reply = replyQuality.buildConcreteContactQuestion(projectContext.getLastUserMessage(session), language);
        if (replyQuality.isAmbiguousChannelReply(session)) reply = replyQuality.buildChannelClarificationQuestion(projectContext.getLastUserMessage(session), language);
        if (replyQuality.lastUserContactButProjectContextMissing(session)) reply = replyQuality.buildProjectFollowUpFallback(session, language);
        if (projectContext.repeatsPreviousAssistantQuestion(reply, session)) reply = replyQuality.buildProjectFollowUpFallback(session, language);

        if (replyQuality.needsStructuredHandoffRecovery(session, reply)) {
            metadata = merge(metadata, openAiClient.extractHandoffMetadata(promptBuilder.handoffMetadataInstructions(config, language), buildConversationForReply(session), reply));
        }
        reply = handoffRenderer.maybeFormatStructuredHandoff(config, session, metadata, reply, language);
        if (replyQuality.replyLooksLikeTemplate(reply) && !projectContext.hasEnoughProjectContextForHandoff(session)) reply = replyQuality.buildProjectFollowUpFallback(session, language);
        if (replyQuality.replyLooksLikeTemplate(reply) && !contactExtractor.conversationHasContactInfo(session.messages())) reply = ChatbotText.t(language, "contactMissing");
        if (languageSafety.replyLooksWrongLanguage(reply, language) || languageSafety.isLanguageLockOnlyReply(reply) || languageSafety.missesLanguageLockAcknowledgement(reply, session, language)) {
            if (languageSafety.missesLanguageLockAcknowledgement(reply, session, language) && replyQuality.replyLooksLikeTemplate(reply) && !languageSafety.replyLooksWrongLanguage(reply, language)) reply = languageSafety.languageLockSentence(language) + "\n\n" + reply;
            else reply = languageSafety.buildLanguageSafeFallback(session, language);
        }
        return replyQuality.enforceFinalReplySafety(reply, language);
    }

    private List<Map<String, Object>> buildConversationForClassification(ChatSession session, String currentMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ConversationMessage message : recentMessages(session.messages())) messages.add(Map.of("role", message.role(), "content", message.content()));
        messages.add(Map.of("role", "user", "content", currentMessage));
        return messages;
    }

    private List<Map<String, String>> buildConversationForReply(ChatSession session) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (ConversationMessage message : recentMessages(session.messages())) messages.add(Map.of("role", message.role(), "content", message.content()));
        return messages;
    }

    private List<ConversationMessage> recentMessages(List<ConversationMessage> messages) {
        int chars = 0;
        List<ConversationMessage> recent = new ArrayList<>();
        for (int index = messages.size() - 1; index >= 0; index -= 1) {
            ConversationMessage message = messages.get(index);
            int length = message.content() == null ? 0 : message.content().length();
            if (recent.size() >= properties.maxContextMessages() || chars + length > properties.maxContextChars()) break;
            recent.add(0, message);
            chars += length;
        }
        return recent;
    }

    private AssistantMetadata merge(AssistantMetadata previous, AssistantMetadata next) {
        return new AssistantMetadata(!next.reply().isBlank() ? next.reply() : previous.reply(), next.readyForHandoff() || previous.readyForHandoff(), firstNonBlank(next.contactEmail(), previous.contactEmail()), firstNonBlank(next.contactPhone(), previous.contactPhone()), firstNonBlank(next.desiredSolution(), previous.desiredSolution()), firstNonBlank(next.leadSummary(), previous.leadSummary()));
    }

    private String firstNonBlank(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value.trim(); return ""; }
}
