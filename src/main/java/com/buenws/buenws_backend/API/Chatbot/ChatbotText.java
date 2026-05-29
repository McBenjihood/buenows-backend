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
                    Map.entry("dailyChatLimit", "Tageslimit für Chat-Nachrichten erreicht. Bitte versuche es später erneut."),
                    Map.entry("tooManySessions", "Zu viele neue Chats. Bitte warte kurz und versuche es erneut."),
                    Map.entry("dailySessionLimit", "Tageslimit für neue Chats erreicht. Bitte versuche es später erneut."),
                    Map.entry("sessionMessageLimit", "Dieser Chat hat das Nachrichtenlimit erreicht. Bitte starte bei Bedarf einen neuen Chat."),
                    Map.entry("missingMessage", "Nachricht fehlt."),
                    Map.entry("shortMessage", "Nachricht ist zu kurz."),
                    Map.entry("messageTooLong", "Nachricht ist zu lang. Bitte kürze sie auf maximal {max} Zeichen."),
                    Map.entry("openAiMissing", "Der Chatbot ist noch nicht mit einem OpenAI API Key verbunden."),
                    Map.entry("openAiInvalid", "Der OpenAI API Key konnte nicht verwendet werden. Bitte prüfe die Server-Konfiguration."),
                    Map.entry("chatUnavailable", "Der Chatbot ist gerade nicht erreichbar. Bitte versuche es später erneut."),
                    Map.entry("rejected", "Dabei kann ich hier nicht helfen. Ich unterstütze nur bei Fragen und Projektanfragen zu {companyName} und den angebotenen digitalen Lösungen."),
                    Map.entry("contactMissing", "Wie lautet Ihre E-Mail-Adresse?"),
                    Map.entry("sensitiveData", "Bitte senden Sie hier keine Passwörter, Zahlungsdaten, Ausweise oder privaten Dokumente. Schreiben Sie die Anfrage bitte ohne diese Daten weiter.")
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
                    Map.entry("contactMissing", "What is your email address?"),
                    Map.entry("sensitiveData", "Please do not send passwords, payment data, ID documents or private documents here. Continue with your request without these details.")
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

    public static String normalizeGermanOutput(String value) {
        if (value == null || value.isBlank()) return value == null ? "" : value;
        return value
                .replaceAll("\\bueber\\b", "über")
                .replaceAll("\\bUeber\\b", "Über")
                .replaceAll("\\buebernehmen\\b", "übernehmen")
                .replaceAll("\\bUebernehmen\\b", "Übernehmen")
                .replaceAll("\\bfuer\\b", "für")
                .replaceAll("\\bFuer\\b", "Für")
                .replaceAll("\\bkoennen\\b", "können")
                .replaceAll("\\bKoennen\\b", "Können")
                .replaceAll("\\bkoennte\\b", "könnte")
                .replaceAll("\\bKoennte\\b", "Könnte")
                .replaceAll("\\bmoechte\\b", "möchte")
                .replaceAll("\\bMoechte\\b", "Möchte")
                .replaceAll("\\bmoechten\\b", "möchten")
                .replaceAll("\\bMoechten\\b", "Möchten")
                .replaceAll("\\bmoeglich\\b", "möglich")
                .replaceAll("\\bMoeglich\\b", "Möglich")
                .replaceAll("\\bmoegliche\\b", "mögliche")
                .replaceAll("\\bMoegliche\\b", "Mögliche")
                .replaceAll("\\bmoeglichkeit\\b", "möglichkeit")
                .replaceAll("\\bMoeglichkeit\\b", "Möglichkeit")
                .replaceAll("\\bKontaktmoeglichkeit\\b", "Kontaktmöglichkeit")
                .replaceAll("\\bLoesung\\b", "Lösung")
                .replaceAll("\\bloesung\\b", "lösung")
                .replaceAll("\\bLoesungen\\b", "Lösungen")
                .replaceAll("\\bloesungen\\b", "lösungen")
                .replaceAll("\\bGewuenschte\\b", "Gewünschte")
                .replaceAll("\\bgewuenschte\\b", "gewünschte")
                .replaceAll("\\bgewuenscht\\b", "gewünscht")
                .replaceAll("\\bwuenschen\\b", "wünschen")
                .replaceAll("\\bWuenschen\\b", "Wünschen")
                .replaceAll("\\bspaeter\\b", "später")
                .replaceAll("\\bSpaeter\\b", "Später")
                .replaceAll("\\bkuerze\\b", "kürze")
                .replaceAll("\\bKuerze\\b", "Kürze")
                .replaceAll("\\bkuerzen\\b", "kürzen")
                .replaceAll("\\bKuerzen\\b", "Kürzen")
                .replaceAll("\\bpruefe\\b", "prüfe")
                .replaceAll("\\bPruefe\\b", "Prüfe")
                .replaceAll("\\bpruefen\\b", "prüfen")
                .replaceAll("\\bPruefen\\b", "Prüfen")
                .replaceAll("\\bunterstuetze\\b", "unterstütze")
                .replaceAll("\\bUnterstuetze\\b", "Unterstütze")
                .replaceAll("\\bunterstuetzen\\b", "unterstützen")
                .replaceAll("\\bUnterstuetzen\\b", "Unterstützen")
                .replaceAll("\\bwaere\\b", "wäre")
                .replaceAll("\\bWaere\\b", "Wäre")
                .replaceAll("\\bbenoetige\\b", "benötige")
                .replaceAll("\\bBenoetige\\b", "Benötige")
                .replaceAll("\\bNaechstes\\b", "Nächstes")
                .replaceAll("\\bnaechstes\\b", "nächstes")
                .replaceAll("\\brueckruf\\b", "rückruf")
                .replaceAll("\\bRueckruf\\b", "Rückruf")
                .replaceAll("\\bPasswoerter\\b", "Passwörter")
                .replaceAll("\\bGespraeche\\b", "Gespräche")
                .replaceAll("\\boeffnen\\b", "öffnen")
                .replaceAll("\\bOeffnen\\b", "Öffnen");
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
