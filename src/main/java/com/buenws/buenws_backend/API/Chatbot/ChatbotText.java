package com.buenws.buenws_backend.API.Chatbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class ChatbotText {
    public static final String DEFAULT_LANGUAGE = "en";
    public static final Set<String> SUPPORTED_LANGUAGES = Set.of("de", "en");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00a0]+");

    private static final Map<String, Map<String, String>> TEXT = Map.of(
            "de", Map.ofEntries(
                    Map.entry("tooManyMessages", "Zu viele Nachrichten. Bitte warte kurz und versuche es erneut."),
                    Map.entry("dailyChatLimit", "Tageslimit fuer Chat-Nachrichten erreicht. Bitte versuche es spaeter erneut."),
                    Map.entry("tooManySessions", "Zu viele neue Chats. Bitte warte kurz und versuche es erneut."),
                    Map.entry("dailySessionLimit", "Tageslimit fuer neue Chats erreicht. Bitte versuche es spaeter erneut."),
                    Map.entry("sessionMessageLimit", "Dieser Chat hat das Nachrichtenlimit erreicht. Bitte starte bei Bedarf einen neuen Chat."),
                    Map.entry("missingMessage", "Nachricht fehlt."),
                    Map.entry("shortMessage", "Nachricht ist zu kurz."),
                    Map.entry("messageTooLong", "Nachricht ist zu lang. Bitte kuerze sie auf maximal {max} Zeichen."),
                    Map.entry("openAiMissing", "Der Chatbot ist noch nicht mit einem OpenAI API Key verbunden."),
                    Map.entry("openAiInvalid", "Der OpenAI API Key konnte nicht verwendet werden. Bitte pruefe die Server-Konfiguration."),
                    Map.entry("chatUnavailable", "Der Chatbot ist gerade nicht erreichbar. Bitte versuche es spaeter erneut."),
                    Map.entry("rejected", "Dabei kann ich hier nicht helfen. Ich unterstuetze nur bei Fragen und Projektanfragen zu {companyName} und den angebotenen digitalen Loesungen."),
                    Map.entry("contactMissing", "Wie k\u00f6nnen wir Sie am besten kontaktieren?")
            ),
            "en", Map.ofEntries(
                    Map.entry("tooManyMessages", "Too many messages. Please wait briefly and try again."),
                    Map.entry("dailyChatLimit", "Daily chat message limit reached. Please try again later."),
                    Map.entry("tooManySessions", "Too many new chats. Please wait briefly and try again."),
                    Map.entry("dailySessionLimit", "Daily limit for new chats reached. Please try again later."),
                    Map.entry("sessionMessageLimit", "This chat has reached the message limit. Please start a new chat if needed."),
                    Map.entry("missingMessage", "Message is missing."),
                    Map.entry("shortMessage", "Message is too short."),
                    Map.entry("messageTooLong", "Message is too long. Please shorten it to a maximum of {max} characters."),
                    Map.entry("openAiMissing", "The chatbot is not connected to an OpenAI API key yet."),
                    Map.entry("openAiInvalid", "The OpenAI API key could not be used. Please check the server configuration."),
                    Map.entry("chatUnavailable", "The chatbot is currently unavailable. Please try again later."),
                    Map.entry("rejected", "I cannot help with that here. I only support questions and project inquiries about {companyName} and the offered digital solutions."),
                    Map.entry("contactMissing", "What is the best way to contact you?")
            )
    );

    private ChatbotText() {}

    public static String resolveLanguage(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_LANGUAGES.contains(normalized) ? normalized : DEFAULT_LANGUAGE;
    }

    public static String t(String language, String key) {
        return t(language, key, Map.of());
    }

    public static String t(String language, String key, Map<String, ?> params) {
        String template = TEXT.getOrDefault(resolveLanguage(language), TEXT.get(DEFAULT_LANGUAGE)).getOrDefault(key, key);
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return template;
    }

    public static String normalizeInputText(String value) {
        return cleanText(value, 4000).trim();
    }

    public static String cleanText(String value, int maxLength) {
        String cleaned = value == null ? "" : CONTROL_CHARS.matcher(value).replaceAll("");
        cleaned = cleaned.replace("\r\n", "\n").replace('\r', '\n');
        return trimToLength(cleaned, maxLength).trim();
    }

    public static String cleanInlineText(String value, int maxLength) {
        return trimToLength(WHITESPACE.matcher(cleanText(value, maxLength)).replaceAll(" "), maxLength).trim();
    }

    public static String cleanReply(String value) {
        String cleaned = cleanText(value, 4000).trim();
        cleaned = cleaned.replaceAll("(?is)^```(?:json)?\\s*", "").replaceAll("(?is)\\s*```$", "");
        return cleaned.trim();
    }

    public static String trimToLength(String value, int maxLength) {
        if (value == null) return "";
        if (maxLength <= 0 || value.length() <= maxLength) return value;
        return value.substring(0, maxLength).trim();
    }

    public static double clamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    public static JsonNode parseJsonObject(ObjectMapper objectMapper, String value) {
        String cleaned = cleanReply(value);
        try {
            return objectMapper.readTree(cleaned);
        } catch (JsonProcessingException ignored) {
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return objectMapper.readTree(cleaned.substring(start, end + 1));
                } catch (JsonProcessingException ignoredAgain) {
                    return objectMapper.createObjectNode();
                }
            }
            return objectMapper.createObjectNode();
        }
    }
}
