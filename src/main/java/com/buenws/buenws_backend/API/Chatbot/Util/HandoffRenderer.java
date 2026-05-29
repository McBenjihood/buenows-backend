package com.buenws.buenws_backend.API.Chatbot.Util;

import com.buenws.buenws_backend.API.Chatbot.Configuration.ChatbotProperties;
import com.buenws.buenws_backend.API.Chatbot.Guard.LanguageSafetyGuard;
import com.buenws.buenws_backend.API.Chatbot.Guard.ReplyQualityGuard;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotCompanyConfig;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.AssistantMetadata;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.ChatSession;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.ContactInfo;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.ConversationMessage;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.HandoffDraft;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class HandoffRenderer {
    public record HandoffResult(String reply, HandoffDraft handoffDraft) {}

    private final ChatbotProperties properties;
    private final ContactExtractor contactExtractor;
    private final ProjectContextEvaluator projectContext;
    private final ReplyQualityGuard replyQuality;
    private final LanguageSafetyGuard languageSafety;

    public HandoffRenderer(ChatbotProperties properties, ContactExtractor contactExtractor, ProjectContextEvaluator projectContext,
                           ReplyQualityGuard replyQuality, LanguageSafetyGuard languageSafety) {
        this.properties = properties;
        this.contactExtractor = contactExtractor;
        this.projectContext = projectContext;
        this.replyQuality = replyQuality;
        this.languageSafety = languageSafety;
    }

    public HandoffResult maybeFormatStructuredHandoff(ChatbotCompanyConfig config, ChatSession session,
                                                      AssistantMetadata metadata, String reply, String language) {
        ContactInfo contact = contactExtractor.extractFromConversation(session.messages());
        if (!contact.hasEmail()) return new HandoffResult(reply, null);
        if (!projectContext.hasEnoughProjectContextForHandoff(session)) return new HandoffResult(reply, null);
        boolean shouldHandoff = metadata.readyForHandoff()
                || replyQuality.replyLooksLikeTemplate(reply)
                || replyQuality.needsStructuredHandoffRecovery(session, reply);
        if (!shouldHandoff) return new HandoffResult(reply, null);
        String desiredSolution = firstNonBlank(safeMetadataText(metadata.desiredSolution(), session), inferDesiredSolution(session, reply, language));
        String leadSummary = firstNonBlank(safeMetadataText(metadata.leadSummary(), session), inferLeadSummary(session, language));
        if (desiredSolution.isBlank() || leadSummary.isBlank()) return new HandoffResult(reply, null);
        return new HandoffResult(
                buildHandoffReply(config, language, contact, desiredSolution, leadSummary, languageSafety.isLanguageSwitchRequest(projectContext.getLastUserMessage(session))),
                buildHandoffDraft(language, contact, desiredSolution, leadSummary)
        );
    }

    private String buildHandoffReply(ChatbotCompanyConfig config, String language, ContactInfo contact,
                                     String desiredSolution, String leadSummary, boolean includeLanguageLock) {
        String target = firstNonBlank(properties.frontendContactUrl(), config.handoff().path("url").asText(""), config.fallbackContact().path("email").asText(""));
        StringBuilder builder = new StringBuilder();
        if (includeLanguageLock) builder.append(languageSafety.languageLockSentence(language)).append("\n\n");
        if ("de".equals(language)) {
            builder.append("Danke, das ist eine klare Anfrage.\n\nIch kann die Anfrage nicht automatisch absenden. Ich habe die Angaben für das Kontaktformular vorbereitet. Bitte öffnen Sie das Kontaktformular, prüfen Sie die Felder und senden Sie die Anfrage ab:\n").append(target).append("\n\nSobald die Anfrage gesendet wurde, werden wir sie bearbeiten und anschliessend über die angegebene E-Mail-Adresse Kontakt aufnehmen.\n\nDiese Angaben werden für das Formular vorbereitet:\n\n");
            builder.append("E-Mail\n").append(contact.email()).append("\n\n");
            builder.append("Gewünschte Lösung\n").append(cleanLeadField(desiredSolution, 160)).append("\n\nNachricht\n").append(cleanLeadSummary(leadSummary));
        } else {
            builder.append("Thank you, this is a clear request.\n\nI cannot submit the request automatically. I have prepared the details for the contact form. Please open the contact form, review the fields and submit the request:\n").append(target).append("\n\nOnce the request has been submitted, we will review it and then contact you through the provided email address.\n\nThese details will be prepared for the form:\n\n");
            builder.append("Email\n").append(contact.email()).append("\n\n");
            builder.append("Desired solution\n").append(cleanLeadField(desiredSolution, 160)).append("\n\nMessage\n").append(cleanLeadSummary(leadSummary));
        }
        return builder.toString().trim();
    }

    private HandoffDraft buildHandoffDraft(String language, ContactInfo contact, String desiredSolution, String leadSummary) {
        String title = cleanLeadField(desiredSolution, 100);
        String message = cleanLeadSummary(leadSummary);
        return new HandoffDraft(
                ChatbotText.cleanInlineText(contact.email(), 254),
                title,
                ChatbotText.cleanText(message, 2000),
                properties.frontendContactUrl()
        );
    }

    private String inferDesiredSolution(ChatSession session, String reply, String language) {
        String text = projectContext.userConversationText(session).toLowerCase(Locale.ROOT);
        if (projectContext.containsAny(text, "invoice", "invoicing", "rechnung", "rechnungen", "subscription", "abo")) return "de".equals(language) ? "Rechnungsautomatisierung per E-Mail" : "Invoice automation with email delivery";
        if (projectContext.containsAny(text, "payment reminder", "zahlungserinnerung", "mahnung", "reminder")) return "de".equals(language) ? "Automatisierte Zahlungserinnerungen" : "Payment reminder automation";
        if (projectContext.containsAny(text, "internal documents", "knowledge", "pdf", "wissens", "dokumente", "quellen")) return "de".equals(language) ? "Interner KI-Wissensassistent" : "AI knowledge assistant";
        if (projectContext.containsAny(text, "phone assistant", "telefonassistent", "callback", "rueckruf", "rückruf")) return "de".equals(language) ? "KI-Telefonassistent" : "AI phone assistant";
        if (projectContext.containsAny(text, "online-shop", "shop", "kaufen", "bestellen", "verkaufen", "produkte", "schuhe")) return "de".equals(language) ? "Online-Shop" : "Online shop";
        if (projectContext.containsAny(text, "chatbot", "chat bot")) return "de".equals(language) ? "Website-Chatbot" : "Website chatbot";
        if (projectContext.containsAny(text, "portal", "customer portal", "kundenportal", "login")) return "de".equals(language) ? "Kundenportal" : "Customer portal";
        if (projectContext.containsAny(text, "backend", "dashboard", "admin")) return "de".equals(language) ? "Backend-System" : "Backend system";
        if (projectContext.containsAny(text, "website", "webseite", "site", "redesign")) return "Website";
        if (projectContext.containsAny(text, "automation", "automatisierung", "automate")) return "de".equals(language) ? "Prozessautomatisierung" : "Process automation";
        return "de".equals(language) ? "Digitale Lösung" : "Digital solution";
    }

    private String inferLeadSummary(ChatSession session, String language) {
        StringBuilder summary = new StringBuilder();
        for (ConversationMessage message : session.messages()) {
            if ("user".equals(message.role())) {
                String userFact = stripContactDetails(message.content());
                if (userFact.isBlank()) continue;
                if (!summary.isEmpty()) summary.append("\n");
                summary.append(userFact);
            }
        }
        String cleaned = cleanLeadSummary(summary.toString());
        return !cleaned.isBlank() ? cleaned : ("de".equals(language) ? "Es geht um eine digitale Projektanfrage." : "This is a digital project inquiry.");
    }

    private String cleanLeadSummary(String value) {
        return ChatbotText.cleanText(value, 900)
                .replaceAll("(?i)[\\w.-]+@[\\w.-]+\\.[a-z]{2,}", "")
                .replaceAll("(?i)\\b(my email is|meine e-mail ist|contact|kontakt|bitte per mail melden)\\b[: ]*", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String stripContactDetails(String value) {
        return ChatbotText.cleanText(value, 900)
                .replaceAll("(?i)[\\w.-]+@[\\w.-]+\\.[a-z]{2,}", " ")
                .replaceAll("(?i)\\b(my email is|meine e-mail ist|meine mail ist|contact|kontakt|bitte per mail melden)\\b[: ]*", " ")
                .replaceAll("(?<!\\w)(?:\\+?\\d[\\d\\s().-]{6,}\\d)(?!\\w)", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanLeadField(String value, int maxLength) {
        return ChatbotText.cleanInlineText(value, maxLength).replaceAll("[.:;]+$", "").trim();
    }

    private String safeMetadataText(String value, ChatSession session) {
        String cleaned = ChatbotText.cleanText(value, 1000).trim();
        if (cleaned.isBlank()) return "";
        return containsUnsupportedScopeAssumption(cleaned, session) ? "" : cleaned;
    }

    private boolean containsUnsupportedScopeAssumption(String value, ChatSession session) {
        String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        String userText = projectContext.userConversationText(session).toLowerCase(Locale.ROOT);
        if (projectContext.containsAny(text, "chatbot", "chat bot", "ki-chatbot", "ki chatbot", "ki-assistent", "ki assistent", "ai chatbot", "ai assistant")
                && !projectContext.containsAny(userText, "chatbot", "chat bot", "ki-chatbot", "ki chatbot", "ki-assistent", "ki assistent", "ai chatbot", "ai assistant")) {
            return true;
        }
        if (projectContext.containsAny(text, "website", "webseite", "site")
                && !projectContext.containsAny(userText, "website", "webseite", "site", "online-shop", "shop")) {
            return true;
        }
        if (projectContext.containsAny(text, "online-shop", "online shop", "e-commerce", "ecommerce")
                && !projectContext.hasEcommerceIntent(session)) {
            return true;
        }
        if (projectContext.containsAny(text, "backend", "dashboard", "portal")
                && !projectContext.containsAny(userText, "backend", "dashboard", "portal", "login", "admin")) {
            return true;
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }
}
