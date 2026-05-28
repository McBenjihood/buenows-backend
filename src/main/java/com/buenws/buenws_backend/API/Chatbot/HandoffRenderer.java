package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.AssistantMetadata;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatSession;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ContactInfo;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ConversationMessage;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class HandoffRenderer {
    private final ContactExtractor contactExtractor;
    private final ProjectContextEvaluator projectContext;
    private final ReplyQualityGuard replyQuality;
    private final LanguageSafetyGuard languageSafety;

    public HandoffRenderer(ContactExtractor contactExtractor, ProjectContextEvaluator projectContext,
                           ReplyQualityGuard replyQuality, LanguageSafetyGuard languageSafety) {
        this.contactExtractor = contactExtractor;
        this.projectContext = projectContext;
        this.replyQuality = replyQuality;
        this.languageSafety = languageSafety;
    }

    public String maybeFormatStructuredHandoff(ChatbotCompanyConfig config, ChatSession session,
                                               AssistantMetadata metadata, String reply, String language) {
        ContactInfo contact = contactExtractor.extractFromConversation(session.messages());
        if (!contact.hasAny()) return reply;
        if (!projectContext.hasEnoughProjectContextForHandoff(session)) return reply;
        boolean shouldHandoff = metadata.readyForHandoff()
                || replyQuality.replyLooksLikeTemplate(reply)
                || replyQuality.needsStructuredHandoffRecovery(session, reply);
        if (!shouldHandoff) return reply;
        String desiredSolution = firstNonBlank(metadata.desiredSolution(), inferDesiredSolution(session, reply, language));
        String leadSummary = firstNonBlank(metadata.leadSummary(), inferLeadSummary(session, language));
        if (desiredSolution.isBlank() || leadSummary.isBlank()) return reply;
        return buildHandoffReply(config, language, contact, desiredSolution, leadSummary, languageSafety.isLanguageSwitchRequest(projectContext.getLastUserMessage(session)));
    }

    private String buildHandoffReply(ChatbotCompanyConfig config, String language, ContactInfo contact,
                                     String desiredSolution, String leadSummary, boolean includeLanguageLock) {
        String target = firstNonBlank(config.handoff().path("url").asText(""), config.fallbackContact().path("email").asText(""), config.fallbackContact().path("phone").asText(""));
        StringBuilder builder = new StringBuilder();
        if (includeLanguageLock) builder.append(languageSafety.languageLockSentence(language)).append("\n\n");
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

    private String inferDesiredSolution(ChatSession session, String reply, String language) {
        String text = (projectContext.conversationText(session) + "\n" + (reply == null ? "" : reply)).toLowerCase(Locale.ROOT);
        if (projectContext.containsAny(text, "invoice", "invoicing", "rechnung", "rechnungen", "subscription", "abo")) return "de".equals(language) ? "Rechnungsautomatisierung per E-Mail" : "Invoice automation with email delivery";
        if (projectContext.containsAny(text, "payment reminder", "zahlungserinnerung", "mahnung", "reminder")) return "de".equals(language) ? "Automatisierte Zahlungserinnerungen" : "Payment reminder automation";
        if (projectContext.containsAny(text, "internal documents", "knowledge", "pdf", "wissens", "dokumente", "quellen")) return "de".equals(language) ? "Interner KI-Wissensassistent" : "AI knowledge assistant";
        if (projectContext.containsAny(text, "phone assistant", "telefonassistent", "callback", "rueckruf", "rückruf")) return "de".equals(language) ? "KI-Telefonassistent" : "AI phone assistant";
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
            if ("user".equals(message.role()) && !contactExtractor.textHasContactInfo(message.content())) {
                if (!summary.isEmpty()) summary.append("\n");
                summary.append(message.content());
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

    private String cleanLeadField(String value, int maxLength) {
        return ChatbotText.cleanInlineText(value, maxLength).replaceAll("[.:;]+$", "").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }
}
