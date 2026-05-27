package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ContactInfo;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ConversationMessage;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ContactExtractor {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.[a-z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_CANDIDATE_PATTERN = Pattern.compile("(?:\\+|00)?\\d[\\d\\s().-]{5,}\\d");
    private static final Pattern CONTACT_CONTEXT_PATTERN = Pattern.compile("\\b(phone|tel|telefon|contact|kontakt|call|mobile|handy|whatsapp)\\b", Pattern.CASE_INSENSITIVE);

    public ContactInfo extractFromConversation(List<ConversationMessage> messages) {
        StringBuilder text = new StringBuilder();
        for (ConversationMessage message : messages) {
            if ("user".equals(message.role())) text.append(message.content()).append('\n');
        }
        return new ContactInfo(extractEmail(text.toString()), extractPhone(text.toString()));
    }

    public boolean conversationHasContactInfo(List<ConversationMessage> messages) {
        return extractFromConversation(messages).hasAny();
    }

    public boolean textHasContactInfo(String value) {
        return !extractEmail(value).isBlank() || !extractPhone(value).isBlank();
    }

    public String extractEmail(String value) {
        Matcher matcher = EMAIL_PATTERN.matcher(value == null ? "" : value);
        return matcher.find() ? matcher.group() : "";
    }

    public String extractPhone(String value) {
        String source = EMAIL_PATTERN.matcher(value == null ? "" : value).replaceAll("");
        Matcher matcher = PHONE_CANDIDATE_PATTERN.matcher(source);
        while (matcher.find()) {
            String candidate = normalizePhoneCandidate(matcher.group());
            if (!candidate.isBlank() && hasPhoneShape(candidate, source, matcher.start())) return candidate;
        }
        return "";
    }

    private String normalizePhoneCandidate(String value) {
        String cleaned = (value == null ? "" : value).replaceAll("^[^\\d+]+|[^\\d)]+$", "").trim();
        String digits = cleaned.replaceAll("\\D", "");
        return digits.length() < 8 || digits.length() > 16 ? "" : cleaned;
    }

    private boolean hasPhoneShape(String candidate, String sourceText, int startIndex) {
        String trimmed = candidate.trim();
        String digits = trimmed.replaceAll("\\D", "");
        boolean hasInternationalPrefix = trimmed.startsWith("+") || trimmed.startsWith("00");
        boolean startsLikeLocalPhone = digits.startsWith("0") && digits.length() >= 9;
        int separatorCount = 0;
        for (int index = 0; index < trimmed.length(); index += 1) {
            char current = trimmed.charAt(index);
            if (Character.isWhitespace(current) || current == '(' || current == ')' || current == '.' || current == '-') separatorCount += 1;
        }
        String context = sourceText.substring(Math.max(0, startIndex - 40), Math.max(0, startIndex));
        return hasInternationalPrefix || startsLikeLocalPhone || separatorCount >= 2 || CONTACT_CONTEXT_PATTERN.matcher(context).find();
    }
}
