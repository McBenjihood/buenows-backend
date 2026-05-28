package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.AssistantMetadata;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatSession;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.Classification;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ClassificationDecision;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ConfigResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ContactInfo;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ConversationMessage;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.HealthResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.SessionResponse;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ChatbotService {
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\b(Gewuenschte Loesung|Gewünschte Lösung|Desired solution|Nachricht|Message)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTACT_ASK_PATTERN = Pattern.compile("\\b(best contact|best way to contact|email address|phone number|Kontaktmoeglichkeit|Kontaktmöglichkeit|E-Mail-Adresse|Telefonnummer|per Mail melden)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BAD_FILLER_PATTERN = Pattern.compile("\\b(Das klingt|Es klingt|Danke fuer die Informationen|Danke für die Informationen|Es waere hilfreich|Es wäre hilfreich|Ich benoetige noch|Ich benötige noch|Moechten Sie|Möchten Sie|Koennten Sie|Könnten Sie|That sounds|It sounds|To clarify|This could involve|It would be helpful|Would you like to|Could you please|we can develop|we could develop)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE_PROMISE_PATTERN = Pattern.compile("\\b(we guarantee|we promise|guaranteed results|guaranteed leads|guaranteed customers|wir garantieren|wir versprechen|garantierte kunden|garantierte anfragen|sicher mehr kunden|sicher mehr anfragen)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LANGUAGE_SWITCH_PATTERN = Pattern.compile("\\b(antworte|antworten|reply|respond|answer)\\b.{0,40}\\b(deutsch|englisch|german|english)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern THANK_YOU_INQUIRY_PATTERN = Pattern.compile("\\b(Vielen Dank fuer Ihre Anfrage|Vielen Dank für Ihre Anfrage|Vielen Dank fÃ¼r Ihre Anfrage|Thank you for your inquiry)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEAK_QUESTION_PATTERN = Pattern.compile("\\b(Could you|K.nnten)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[[^\\]]+]\\(https?://[^)]+\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EARLY_HANDOFF_PATTERN = Pattern.compile("\\b(contact form|contact page|project request|Projekt anfragen|Kontaktformular|Kontaktseite|Formular|Anfrageformular|Handoff)\\b|https?://\\S+", Pattern.CASE_INSENSITIVE);

    private final ChatbotProperties properties;
    private final ChatbotCompanyConfigService companyConfigService;
    private final ChatbotSessionStore sessionStore;
    private final OpenAiResponsesClient openAiClient;
    private final PromptBuilder promptBuilder;
    private final ContactExtractor contactExtractor;
    private final ChatbotConversationHistoryService historyService;

    public ChatbotService(ChatbotProperties properties, ChatbotCompanyConfigService companyConfigService,
                          ChatbotSessionStore sessionStore, OpenAiResponsesClient openAiClient,
                          PromptBuilder promptBuilder, ContactExtractor contactExtractor,
                          ChatbotConversationHistoryService historyService) {
        this.properties = properties;
        this.companyConfigService = companyConfigService;
        this.sessionStore = sessionStore;
        this.openAiClient = openAiClient;
        this.promptBuilder = promptBuilder;
        this.contactExtractor = contactExtractor;
        this.historyService = historyService;
    }

    public ConfigResponse publicConfig(String language) { return companyConfigService.publicConfig(language); }
    public SessionResponse createSession(String language) {
        ChatSession session = sessionStore.create(language);
        return new SessionResponse(session.id(), session.language());
    }

    public String resolveLanguageForRequest(String requestedSessionId, String requestedLanguage) {
        ChatSession session = sessionStore.find(requestedSessionId);
        return session == null ? ChatbotText.resolveLanguage(requestedLanguage) : session.language();
    }

    public ChatResponse chat(String requestedSessionId, String requestedLanguage, String rawMessage) {
        String requestLanguage = ChatbotText.resolveLanguage(requestedLanguage);
        String userMessage = ChatbotText.normalizeInputText(rawMessage);
        if (userMessage.isBlank()) throw new ChatbotValidationException(ChatbotText.t(requestLanguage, "missingMessage"));
        if (userMessage.length() < 2) throw new ChatbotValidationException(ChatbotText.t(requestLanguage, "shortMessage"));
        if (userMessage.length() > properties.maxMessageLength()) throw new ChatbotValidationException(ChatbotText.t(requestLanguage, "messageTooLong", Map.of("max", properties.maxMessageLength())));

        ChatSession session = sessionStore.find(requestedSessionId);
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
            String status = replyLooksLikeTemplate(reply)
                    ? ChatbotConversationHistoryService.STATUS_COMPLETED
                    : ChatbotConversationHistoryService.STATUS_ACTIVE;
            historyService.saveAssistantMessage(config, session, reply, status);
            return new ChatResponse(reply, session.id(), language, false);
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
            String issue = getReplyQualityIssue(reply, session, metadata, language);
            if (issue == null) break;
            metadata = merge(metadata, openAiClient.repairReply(promptBuilder.repairInstructions(config, classification, language, issue), buildConversationForReply(session), issue, reply));
            reply = ChatbotText.cleanReply(metadata.reply());
        }
        if (needsEarlyProjectFallback(reply, session) || needsWeakProjectQuestionFallback(reply, session)) reply = buildProjectFollowUpFallback(session, language);

        if (needsStructuredHandoffRecovery(session, reply)) {
            metadata = merge(metadata, openAiClient.extractHandoffMetadata(promptBuilder.handoffMetadataInstructions(config, language), buildConversationForReply(session), reply));
        }
        reply = maybeFormatStructuredHandoff(config, session, metadata, reply, language);
        if (replyLooksLikeTemplate(reply) && !contactExtractor.conversationHasContactInfo(session.messages())) reply = ChatbotText.t(language, "contactMissing");
        if (replyLooksWrongLanguage(reply, language) || isLanguageLockOnlyReply(reply) || missesLanguageLockAcknowledgement(reply, session, language)) {
            if (missesLanguageLockAcknowledgement(reply, session, language) && replyLooksLikeTemplate(reply) && !replyLooksWrongLanguage(reply, language)) reply = languageLockSentence(language) + "\n\n" + reply;
            else reply = buildLanguageSafeFallback(session, language);
        }
        return enforceFinalReplySafety(reply, language);
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

    private String maybeFormatStructuredHandoff(ChatbotCompanyConfig config, ChatSession session, AssistantMetadata metadata, String reply, String language) {
        ContactInfo contact = contactExtractor.extractFromConversation(session.messages());
        if (!contact.hasAny()) return reply;
        boolean shouldHandoff = metadata.readyForHandoff() || replyLooksLikeTemplate(reply) || needsStructuredHandoffRecovery(session, reply);
        if (!shouldHandoff) return reply;
        String desiredSolution = firstNonBlank(metadata.desiredSolution(), inferDesiredSolution(session, reply, language));
        String leadSummary = firstNonBlank(metadata.leadSummary(), inferLeadSummary(session, language));
        if (desiredSolution.isBlank() || leadSummary.isBlank()) return reply;
        return buildHandoffReply(config, language, contact, desiredSolution, leadSummary, isLanguageSwitchRequest(getLastUserMessage(session)));
    }

    private String buildHandoffReply(ChatbotCompanyConfig config, String language, ContactInfo contact, String desiredSolution, String leadSummary, boolean includeLanguageLock) {
        String target = firstNonBlank(config.handoff().path("url").asText(""), config.fallbackContact().path("email").asText(""), config.fallbackContact().path("phone").asText(""));
        StringBuilder builder = new StringBuilder();
        if (includeLanguageLock) builder.append(languageLockSentence(language)).append("\n\n");
        if ("de".equals(language)) {
            builder.append("Danke, das ist eine klare Anfrage.\n\nIch kann Ihre Angaben nicht automatisch ins Kontaktformular eintragen oder ans Team senden.\nBitte senden Sie die Anfrage deshalb über das Kontaktformular:\n").append(target).append("\n\nSobald die Anfrage gesendet wurde, werden wir sie bearbeiten und anschliessend über die angegebene Kontaktmöglichkeit Kontakt aufnehmen.\n\nFür das Formular können Sie diese Angaben übernehmen:\n\n");
            if (!contact.email().isBlank()) builder.append("E-Mail\n").append(contact.email()).append("\n\n");
            if (!contact.phone().isBlank()) builder.append("Telefon\n").append(contact.phone()).append("\n\n");
            builder.append("Gewünschte Lösung\n").append(cleanLeadField(desiredSolution, 160)).append("\n\nNachricht\n").append(cleanLeadSummary(leadSummary));
        } else {
            builder.append("Thank you, this is a clear request.\n\nI cannot automatically submit your details through the contact form or send them to the team.\nPlease submit the request via the contact form:\n").append(target).append("\n\nOnce the request has been submitted, we will review it and then contact you through the provided contact details.\n\nYou can use these details for the form:\n\n");
            if (!contact.email().isBlank()) builder.append("Email\n").append(contact.email()).append("\n\n");
            if (!contact.phone().isBlank()) builder.append("Phone\n").append(contact.phone()).append("\n\n");
            builder.append("Desired solution\n").append(cleanLeadField(desiredSolution, 160)).append("\n\nMessage\n").append(cleanLeadSummary(leadSummary));
        }
        return builder.toString().trim();
    }

    private String getReplyQualityIssue(String reply, ChatSession session, AssistantMetadata metadata, String language) {
        if (replyLooksWrongLanguage(reply, language)) return "The reply is not in the fixed session language.";
        if (MARKDOWN_LINK_PATTERN.matcher(reply == null ? "" : reply).find()) return "The reply uses a Markdown link. Use plain text only. Do not link to the contact form before final handoff.";
        if (needsEarlyProjectFallback(reply, session)) return "The reply redirects to the contact form too early. Continue the conversation by asking one practical follow-up question in chat.";
        if (metadata.readyForHandoff() && !replyLooksLikeTemplate(reply)) return "The metadata says handoff is ready but the reply does not use the exact contact-form template.";
        if (contactExtractor.conversationHasContactInfo(session.messages()) && asksForContactOption(reply)) return "The user already provided contact information. Generate the contact-form template if the project is concrete enough.";
        if (lastMessageHasContactAndLanguageSwitch(session) && !replyLooksLikeTemplate(reply)) return "The user asked to switch language and provided contact information. Keep the fixed language and generate the template if context is clear.";
        if (BAD_FILLER_PATTERN.matcher(reply == null ? "" : reply).find() || THANK_YOU_INQUIRY_PATTERN.matcher(reply == null ? "" : reply).find() || WEAK_QUESTION_PATTERN.matcher(reply == null ? "" : reply).find()) return "The reply uses filler or weak customer-service phrasing. Ask directly without 'Could you' or 'Könnten Sie'.";
        if (UNSAFE_PROMISE_PATTERN.matcher(reply == null ? "" : reply).find()) return "The reply makes a promise or guarantee.";
        return null;
    }

    private boolean needsStructuredHandoffRecovery(ChatSession session, String reply) {
        return contactExtractor.conversationHasContactInfo(session.messages()) && !replyLooksLikeTemplate(reply) && (lastMessageHasContactAndLanguageSwitch(session) || asksForContactOption(reply) || asksOptionalDetailAfterContact(session, reply));
    }
    private boolean needsEarlyProjectFallback(String reply, ChatSession session) {
        String value = reply == null ? "" : reply;
        return !contactExtractor.conversationHasContactInfo(session.messages())
                && !replyLooksLikeTemplate(value)
                && EARLY_HANDOFF_PATTERN.matcher(value).find();
    }
    private boolean needsWeakProjectQuestionFallback(String reply, ChatSession session) {
        String value = reply == null ? "" : reply;
        return !contactExtractor.conversationHasContactInfo(session.messages())
                && !replyLooksLikeTemplate(value)
                && getUserMessageCount(session) <= 1
                && (BAD_FILLER_PATTERN.matcher(value).find() || THANK_YOU_INQUIRY_PATTERN.matcher(value).find() || WEAK_QUESTION_PATTERN.matcher(value).find());
    }
    private boolean replyLooksLikeTemplate(String reply) { return TEMPLATE_PATTERN.matcher(reply == null ? "" : reply).find(); }
    private boolean asksForContactOption(String reply) { return CONTACT_ASK_PATTERN.matcher(reply == null ? "" : reply).find() && (reply == null || reply.contains("?")); }
    private boolean asksOptionalDetailAfterContact(ChatSession session, String reply) { return contactExtractor.conversationHasContactInfo(session.messages()) && getUserMessageCount(session) >= 2 && reply != null && reply.contains("?") && !asksForContactOption(reply); }
    private boolean lastMessageHasContactAndLanguageSwitch(ChatSession session) { String last = getLastUserMessage(session); return contactExtractor.textHasContactInfo(last) && isLanguageSwitchRequest(last); }
    private int getUserMessageCount(ChatSession session) { int count = 0; for (ConversationMessage message : session.messages()) if ("user".equals(message.role())) count += 1; return count; }
    private boolean replyLooksWrongLanguage(String reply, String language) {
        if (reply == null || reply.isBlank()) return false;
        return "de".equals(language)
                ? Pattern.compile("\\b(Please|What current|Could you|Desired solution|The session language|Thank you, this is a clear request)\\b", Pattern.CASE_INSENSITIVE).matcher(reply).find()
                : Pattern.compile("\\b(Bitte|Welche|Koennen|Können|Gewuenschte Loesung|Gewünschte Lösung|Die Sitzungssprache|Danke, das ist eine klare Anfrage)\\b", Pattern.CASE_INSENSITIVE).matcher(reply).find();
    }
    private boolean isLanguageLockOnlyReply(String reply) { return reply != null && Pattern.compile("\\b(session language stays fixed|Sitzungssprache bleibt)\\b", Pattern.CASE_INSENSITIVE).matcher(reply).find() && reply.trim().length() < 120; }
    private boolean missesLanguageLockAcknowledgement(String reply, ChatSession session, String language) {
        if (!isLanguageSwitchRequest(getLastUserMessage(session))) return false;
        return "de".equals(language)
                ? !Pattern.compile("\\bSitzungssprache bleibt Deutsch\\b", Pattern.CASE_INSENSITIVE).matcher(reply == null ? "" : reply).find()
                : !Pattern.compile("\\bsession language stays fixed in English\\b", Pattern.CASE_INSENSITIVE).matcher(reply == null ? "" : reply).find();
    }
    private boolean isLanguageSwitchRequest(String value) { return LANGUAGE_SWITCH_PATTERN.matcher(value == null ? "" : value).find(); }
    private String languageLockSentence(String language) { return "de".equals(language) ? "Die Sitzungssprache bleibt Deutsch." : "The session language stays fixed in English."; }
    private String buildLanguageSafeFallback(ChatSession session, String language) { String question = getLastAssistantQuestion(session); return !question.isBlank() ? question : ("de".equals(language) ? "Welche Information ist für die Anfrage als Nächstes am wichtigsten?" : "What information is most important for the request next?"); }
    private String buildProjectFollowUpFallback(ChatSession session, String language) {
        String text = conversationText(session).toLowerCase(Locale.ROOT);
        if (containsAny(text, "rechnung", "rechnungen", "invoice", "invoicing")) {
            return "de".equals(language)
                    ? "Welche Software oder welchen aktuellen Ablauf nutzen Sie heute f\u00fcr die Rechnungen?"
                    : "What software or current process do you use for invoicing today?";
        }
        if (containsAny(text, "website", "webseite", "redesign")) {
            return "de".equals(language)
                    ? "Gibt es bereits eine bestehende Website, die erneuert werden soll?"
                    : "Is there already an existing website that should be redesigned?";
        }
        if (containsAny(text, "automation", "automatisierung", "automate", "automatisieren")) {
            return "de".equals(language)
                    ? "Welcher Ablauf soll als Erstes automatisiert werden?"
                    : "Which process should be automated first?";
        }
        return buildLanguageSafeFallback(session, language);
    }
    private String enforceFinalReplySafety(String reply, String language) {
        String cleaned = ChatbotText.cleanReply(reply)
                .replaceAll("(?i)\\bwe guarantee\\b", "we can discuss")
                .replaceAll("(?i)\\bguaranteed\\b", "planned")
                .replaceAll("(?i)\\bwir garantieren\\b", "wir können prüfen")
                .trim();
        return "de".equals(language) ? ChatbotText.normalizeGermanOutput(cleaned) : cleaned;
    }
    private String inferDesiredSolution(ChatSession session, String reply, String language) {
        String text = (conversationText(session) + "\n" + (reply == null ? "" : reply)).toLowerCase(Locale.ROOT);
        if (containsAny(text, "invoice", "invoicing", "rechnung", "rechnungen", "subscription", "abo")) return "de".equals(language) ? "Rechnungsautomatisierung per E-Mail" : "Invoice automation with email delivery";
        if (containsAny(text, "payment reminder", "zahlungserinnerung", "mahnung", "reminder")) return "de".equals(language) ? "Automatisierte Zahlungserinnerungen" : "Payment reminder automation";
        if (containsAny(text, "internal documents", "knowledge", "pdf", "wissens", "dokumente", "quellen")) return "de".equals(language) ? "Interner KI-Wissensassistent" : "AI knowledge assistant";
        if (containsAny(text, "phone assistant", "telefonassistent", "callback", "rueckruf", "rückruf")) return "de".equals(language) ? "KI-Telefonassistent" : "AI phone assistant";
        if (containsAny(text, "chatbot", "chat bot")) return "de".equals(language) ? "Website-Chatbot" : "Website chatbot";
        if (containsAny(text, "portal", "customer portal", "kundenportal", "login")) return "de".equals(language) ? "Kundenportal" : "Customer portal";
        if (containsAny(text, "backend", "dashboard", "admin")) return "de".equals(language) ? "Backend-System" : "Backend system";
        if (containsAny(text, "website", "webseite", "site", "redesign")) return "Website";
        if (containsAny(text, "automation", "automatisierung", "automate")) return "de".equals(language) ? "Prozessautomatisierung" : "Process automation";
        return "de".equals(language) ? "Digitale Lösung" : "Digital solution";
    }
    private String inferLeadSummary(ChatSession session, String language) {
        StringBuilder summary = new StringBuilder();
        for (ConversationMessage message : session.messages()) {
            if ("user".equals(message.role()) && !contactExtractor.textHasContactInfo(message.content())) {
                if (!summary.isEmpty()) summary.append("\n");
                summary.append(message.content());
            }
        }
        String cleaned = cleanLeadSummary(summary.toString());
        return !cleaned.isBlank() ? cleaned : ("de".equals(language) ? "Es geht um eine digitale Projektanfrage." : "This is a digital project inquiry.");
    }
    private String cleanLeadSummary(String value) { return ChatbotText.cleanText(value, 900).replaceAll("(?i)[\\w.-]+@[\\w.-]+\\.[a-z]{2,}", "").replaceAll("(?i)\\b(my email is|meine e-mail ist|contact|kontakt|bitte per mail melden)\\b[: ]*", "").replaceAll("\\n{3,}", "\n\n").trim(); }
    private String cleanLeadField(String value, int maxLength) { return ChatbotText.cleanInlineText(value, maxLength).replaceAll("[.:;]+$", "").trim(); }
    private String conversationText(ChatSession session) { StringBuilder builder = new StringBuilder(); for (ConversationMessage message : session.messages()) builder.append(message.content()).append('\n'); return builder.toString(); }
    private boolean containsAny(String text, String... needles) { for (String needle : needles) if (text.contains(needle)) return true; return false; }
    private String getLastAssistantQuestion(ChatSession session) { for (int i = session.messages().size() - 1; i >= 0; i--) { ConversationMessage m = session.messages().get(i); if ("assistant".equals(m.role()) && m.content().contains("?")) return m.content(); } return ""; }
    private String getLastUserMessage(ChatSession session) { for (int i = session.messages().size() - 1; i >= 0; i--) { ConversationMessage m = session.messages().get(i); if ("user".equals(m.role())) return m.content(); } return ""; }
    private String firstNonBlank(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value.trim(); return ""; }
}
