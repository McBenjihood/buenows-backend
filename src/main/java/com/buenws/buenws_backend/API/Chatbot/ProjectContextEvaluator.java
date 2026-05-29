package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatSession;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ConversationMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ProjectContextEvaluator {
    private static final Pattern CONTACT_PREFERENCE_PATTERN = Pattern.compile("\\b(sms|text message|telefon|phone|call|anruf|whatsapp|e-mail|email|mail)\\b", Pattern.CASE_INSENSITIVE);

    private final ContactExtractor contactExtractor;

    public ProjectContextEvaluator(ContactExtractor contactExtractor) {
        this.contactExtractor = contactExtractor;
    }

    public boolean hasEnoughProjectContextForHandoff(ChatSession session) {
        List<String> fragments = projectContextFragments(session);
        if (fragments.isEmpty()) return false;
        String text = String.join(" ", fragments).toLowerCase(Locale.ROOT);
        if (isGenericAutomationInterest(text)) return false;
        if (hasUnscopedBookingOrSupportIntent(text) && !hasScopedDigitalSolutionIntent(text)) return false;
        if (hasEcommerceIntent(text)) return hasEcommerceHandoffDetail(text);
        if (hasInvoiceAutomationIntent(text) && (hasCurrentProcessDetail(text) || hasEmailWorkflowDetail(text))) return true;
        if (hasWebsiteChatbotIntent(text) && hasChatbotPurpose(text)) return true;
        if (hasWebsiteIntent(text) && !hasWebsiteChatbotIntent(text) && (hasWebsiteFunctionDetail(text) || hasCurrentProcessDetail(text) || wordCount(text) >= 12)) return true;
        if (hasBackendIntent(text) && hasConcreteProjectDetail(text)) return true;
        if (hasAutomationIntent(text) && hasConcreteProjectDetail(text) && wordCount(text) >= 6) return true;
        return fragments.size() >= 2 && wordCount(text) >= 14 && hasConcreteProjectDetail(text);
    }

    public boolean hasWebsiteChatbotIntent(ChatSession session) {
        return hasWebsiteChatbotIntent(userConversationText(session).toLowerCase(Locale.ROOT));
    }

    public boolean hasChatbotPurpose(ChatSession session) {
        return hasChatbotPurpose(userConversationText(session).toLowerCase(Locale.ROOT));
    }

    public boolean hasEcommerceIntent(ChatSession session) {
        return hasEcommerceIntent(userConversationText(session).toLowerCase(Locale.ROOT));
    }

    public boolean hasPaymentInfo(ChatSession session) {
        String text = userConversationText(session).toLowerCase(Locale.ROOT);
        return containsAny(text, "karte", "card", "paypal", "google pay", "apple pay", "twint", "rechnung", "crypto", "krypto", "paysafecard", "stripe");
    }

    public boolean hasProductScaleInfo(ChatSession session) {
        String text = userConversationText(session).toLowerCase(Locale.ROOT);
        return hasProductScaleInfo(text);
    }

    public boolean hasEcommerceStartInfo(ChatSession session) {
        return hasTimeframeInfo(userConversationText(session).toLowerCase(Locale.ROOT));
    }

    public boolean hasWebsiteOrSystemStatusInfo(ChatSession session) {
        String text = userConversationText(session).toLowerCase(Locale.ROOT);
        return hasAnsweredNoToExistingWebsiteQuestion(session)
                || hasCurrentProcessDetail(text)
                || containsAny(text, "bestehende website", "bestehende webseite", "existing website", "current system", "aktuelles system", "bestehendes system");
    }

    public boolean lastUserRequestsHumanContact(ChatSession session) {
        String lastUserMessage = getLastUserMessage(session);
        if (contactExtractor.textHasContactLikeValue(lastUserMessage)) return false;
        String text = lastUserMessage.toLowerCase(Locale.ROOT);
        return containsAny(text,
                "echte person", "echten person", "echten menschen", "person reden", "mit jemandem reden", "mit einer person", "menschlicher support",
                "kontakt aufnehmen", "kontaktdaten", "telefonnummer", "e-mail adresse", "email adresse",
                "termin vereinbaren", "meeting vereinbaren", "inhaber", "call vereinbaren",
                "real person", "human", "talk to someone", "contact details", "phone number", "schedule a call");
    }

    public boolean isContactPreferenceWithoutConcreteInfo(String value) {
        String text = value == null ? "" : value.trim();
        String normalized = text.toLowerCase(Locale.ROOT);
        return !contactExtractor.textHasContactInfo(text)
                && text.length() <= 80
                && CONTACT_PREFERENCE_PATTERN.matcher(text).find()
                && !hasChannelWorkflowContext(normalized);
    }

    public boolean repeatsPreviousAssistantQuestion(String reply, ChatSession session) {
        String current = normalizeQuestion(reply);
        String previous = normalizeQuestion(getLastAssistantQuestion(session));
        return !current.isBlank() && !previous.isBlank() && (current.equals(previous) || current.contains(previous) || previous.contains(current));
    }

    public boolean hasAnsweredNoToExistingWebsiteQuestion(ChatSession session) {
        List<ConversationMessage> messages = session.messages();
        for (int i = messages.size() - 1; i >= 1; i--) {
            ConversationMessage message = messages.get(i);
            if (!"user".equals(message.role())) continue;
            String answer = normalizeQuestion(message.content());
            if (!containsAny(answer, "nein", "no", "keine", "nichts", "gar nichts")) continue;
            ConversationMessage previous = messages.get(i - 1);
            if ("assistant".equals(previous.role()) && asksAboutExistingWebsite(previous.content())) return true;
        }
        return false;
    }

    public int getUserMessageCount(ChatSession session) {
        int count = 0;
        for (ConversationMessage message : session.messages()) if ("user".equals(message.role())) count += 1;
        return count;
    }

    public String getLastAssistantQuestion(ChatSession session) {
        for (int i = session.messages().size() - 1; i >= 0; i--) {
            ConversationMessage message = session.messages().get(i);
            if ("assistant".equals(message.role()) && message.content().contains("?")) return message.content();
        }
        return "";
    }

    public String getLastUserMessage(ChatSession session) {
        for (int i = session.messages().size() - 1; i >= 0; i--) {
            ConversationMessage message = session.messages().get(i);
            if ("user".equals(message.role())) return message.content();
        }
        return "";
    }

    public String conversationText(ChatSession session) {
        StringBuilder builder = new StringBuilder();
        for (ConversationMessage message : session.messages()) builder.append(message.content()).append('\n');
        return builder.toString();
    }

    public String userConversationText(ChatSession session) {
        StringBuilder builder = new StringBuilder();
        for (ConversationMessage message : session.messages()) {
            if ("user".equals(message.role())) builder.append(message.content()).append('\n');
        }
        return builder.toString();
    }

    public boolean hasUnscopedBookingOrSupportIntent(ChatSession session) {
        return hasUnscopedBookingOrSupportIntent(userConversationText(session).toLowerCase(Locale.ROOT));
    }

    public boolean hasScopedDigitalSolutionIntent(String text) {
        return hasWebsiteIntent(text)
                || hasAutomationIntent(text)
                || hasBackendIntent(text)
                || hasInvoiceAutomationIntent(text)
                || containsAny(text, "chatbot", "chat bot", "ki-assistent", "ki assistent", "ai assistant", "tool", "system", "plattform", "platform");
    }

    public boolean containsAny(String text, String... needles) {
        if (text == null) return false;
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    public boolean hasCurrentProcessDetail(String text) {
        return containsAny(text, "manuell", "manual", "aktuell", "current", "heute", "bisher", "bestehend", "veraltet", "gar nichts", "keine website", "kein tool", "gmail");
    }

    public String normalizeQuestion(String value) {
        String text = value == null ? "" : value;
        int questionMark = text.indexOf('?');
        if (questionMark >= 0) text = text.substring(0, questionMark + 1);
        return text.toLowerCase(Locale.ROOT)
                .replace('ä', 'a')
                .replace('ö', 'o')
                .replace('ü', 'u')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private List<String> projectContextFragments(ChatSession session) {
        List<String> fragments = new ArrayList<>();
        for (ConversationMessage message : session.messages()) {
            if (!"user".equals(message.role())) continue;
            String cleaned = stripContactInfo(message.content());
            if (cleaned.isBlank() || isContactPreferenceWithoutConcreteInfo(cleaned)) continue;
            fragments.add(cleaned);
        }
        return fragments;
    }

    private String stripContactInfo(String value) {
        return (value == null ? "" : value)
                .replaceAll("(?i)[\\w.-]+@[\\w.-]+\\.[a-z]{2,}", " ")
                .replaceAll("(?i)\\b(kontakt|contact|meine telefon nummer ist|meine telefonnummer ist|telefonnummer|phone number|per mail|per e-mail|my email is)\\b[: ]*", " ")
                .replaceAll("(?<!\\w)(?:\\+?\\d[\\d\\s().-]{6,}\\d)(?!\\w)", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int wordCount(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isBlank()) return 0;
        return cleaned.split("\\s+").length;
    }

    private boolean isGenericAutomationInterest(String text) {
        return wordCount(text) <= 12
                && hasAutomationIntent(text)
                && !hasConcreteProjectDetail(text)
                && !hasWebsiteIntent(text)
                && !hasBackendIntent(text);
    }

    private boolean hasWebsiteIntent(String text) {
        return containsAny(text, "website", "webseite", "site", "redesign", "online-shop", "shop");
    }

    private boolean hasWebsiteChatbotIntent(String text) {
        return hasWebsiteIntent(text) && containsAny(text, "chatbot", "chat bot", "ki assistent", "ki-assistent", "ai assistant", "assistent");
    }

    private boolean hasAutomationIntent(String text) {
        return containsAny(text, "ki", "ai", "automatisierung", "automation", "automate", "automatisieren", "automatisch");
    }

    private boolean hasBackendIntent(String text) {
        return containsAny(text, "backend", "dashboard", "admin", "login", "portal", "datenbank", "database");
    }

    private boolean hasInvoiceAutomationIntent(String text) {
        return containsAny(text, "rechnung", "rechnungen", "invoice", "invoicing", "zahlungserinnerung", "mahnung");
    }

    private boolean hasEmailWorkflowDetail(String text) {
        return containsAny(text, "gmail", "kunden", "senden", "versenden", "schicken", "antwort", "antworten", "anfragen")
                || (containsAny(text, "email", "e-mail", "mail")
                && containsAny(text, "automatisch", "automation", "automatisierung", "rechnung", "rechnungen", "invoice", "invoicing", "kunden", "senden", "versenden", "schicken", "antworten", "beantworten", "anfragen"));
    }

    private boolean hasChannelWorkflowContext(String text) {
        return containsAny(text,
                "automatisierung", "automation", "automatisch", "automate", "workflow", "ablauf", "prozess",
                "rechnung", "rechnungen", "invoice", "invoicing", "mahnung", "zahlungserinnerung",
                "versenden", "senden", "schicken", "erstellen", "generieren", "antworten", "beantworten",
                "kunden", "customer", "anfragen", "tool", "system", "chatbot", "assistent", "terminvereinbarung", "kundenservice");
    }

    private boolean hasWebsiteFunctionDetail(String text) {
        return containsAny(text,
                "kontaktformular", "terminbuchung", "booking", "kundengewinnung", "leads", "shop", "online-shop",
                "kaufen", "bestellen", "verkaufen", "produkte", "schuhe");
    }

    private boolean hasChatbotPurpose(String text) {
        return containsAny(text,
                "kundenservice", "support", "probleme", "fragen beantworten", "termine", "terminbuchung",
                "produkte", "produkt", "anfragen", "antworten", "faq", "technische probleme",
                "customer service", "appointment booking", "appointments", "booking", "answer questions",
                "answering questions", "support requests", "technical issues", "product questions", "visitors", "customers");
    }

    private boolean hasUnscopedBookingOrSupportIntent(String text) {
        return containsAny(text, "terminvereinbarung", "termin vereinbarung", "termine", "appointment", "booking", "kundenservice", "customer service", "support")
                && !hasScopedDigitalSolutionIntent(text);
    }

    private boolean hasEcommerceIntent(String text) {
        return hasWebsiteIntent(text) && containsAny(text, "kaufen", "bestellen", "verkaufen", "online-shop", "shop", "produkte", "schuhe", "payment", "zahlung");
    }

    private boolean hasEcommerceHandoffDetail(String text) {
        return hasPaymentInfo(text) || hasProductScaleInfo(text) || hasTimeframeInfo(text) || hasCurrentProcessDetail(text);
    }

    private boolean hasPaymentInfo(String text) {
        return containsAny(text, "karte", "card", "paypal", "google pay", "apple pay", "twint", "rechnung", "crypto", "krypto", "paysafecard", "stripe");
    }

    private boolean hasProductScaleInfo(String text) {
        return Pattern.compile("\\b\\d{1,5}\\b").matcher(text).find()
                || containsAny(text, "viele produkte", "wenige produkte", "sortiment", "modelle", "produkte");
    }

    private boolean hasTimeframeInfo(String text) {
        return containsAny(text,
                "sofort", "bald", "monat", "monaten", "woche", "wochen", "jahr", "quartal", "starten", "deadline", "zeitraum",
                "as soon", "next month", "month", "months", "week", "weeks", "year", "quarter", "timeline", "timeframe");
    }

    private boolean hasConcreteProjectDetail(String text) {
        return hasInvoiceAutomationIntent(text)
                || hasEmailWorkflowDetail(text)
                || hasWebsiteFunctionDetail(text)
                || hasChatbotPurpose(text)
                || hasBackendIntent(text)
                || containsAny(text, "dokument", "pdf", "wissens", "knowledge", "telefonassistent", "phone assistant", "abo", "subscription", "kundenanfragen");
    }

    private boolean asksAboutExistingWebsite(String value) {
        String normalized = normalizeQuestion(value);
        return containsAny(normalized, "bestehende website", "bestehende webseite", "existing website", "website erneuert", "webseite erneuert", "redesigned");
    }
}
