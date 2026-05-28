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
        if (containsAny(text,
                "rechnung", "rechnungen", "invoice", "invoicing", "zahlung", "mahnung", "abo", "subscription",
                "website", "webseite", "redesign", "terminbuchung", "booking", "kundengewinnung", "leads",
                "backend", "dashboard", "admin", "login", "portal", "datenbank", "database",
                "gmail", "e-mail", "email", "mail", "antwort", "support", "kundensupport", "chatbot",
                "telefonassistent", "phone assistant", "dokument", "pdf", "wissens", "knowledge",
                "manuell", "manual", "current process", "aktueller ablauf", "bestehend", "veraltet")) return true;
        if (fragments.size() >= 2 && wordCount(text) >= 12) return true;
        return wordCount(text) >= 18 && containsAny(text, "ki", "ai", "automatisierung", "automation", "automate", "automatisieren");
    }

    public boolean isContactPreferenceWithoutConcreteInfo(String value) {
        String text = value == null ? "" : value.trim();
        return !contactExtractor.textHasContactInfo(text) && text.length() <= 80 && CONTACT_PREFERENCE_PATTERN.matcher(text).find();
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

    public boolean containsAny(String text, String... needles) {
        if (text == null) return false;
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
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

    private boolean asksAboutExistingWebsite(String value) {
        String normalized = normalizeQuestion(value);
        return containsAny(normalized, "bestehende website", "bestehende webseite", "existing website", "website erneuert", "webseite erneuert", "redesigned");
    }
}
