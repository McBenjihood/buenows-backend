package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ConfigResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotCompanyConfigService {
    private static final String RESOURCE_CONFIG = "chatbot/company.config.json";
    private final ChatbotProperties properties;
    private final ObjectMapper objectMapper;
    private volatile ChatbotCompanyConfig cachedConfig;
    private volatile long cachedMtime = -1;

    public ChatbotCompanyConfigService(ChatbotProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public ChatbotCompanyConfig loadConfig() {
        try {
            Path externalPath = properties.companyConfigPath();
            if (externalPath != null) {
                long mtime = Files.getLastModifiedTime(externalPath).toMillis();
                if (cachedConfig != null && cachedMtime == mtime) return cachedConfig;
                try (InputStream inputStream = Files.newInputStream(externalPath)) {
                    cachedConfig = normalize(objectMapper.readTree(inputStream));
                    cachedMtime = mtime;
                    return cachedConfig;
                }
            }
            if (cachedConfig != null) return cachedConfig;
            try (InputStream inputStream = new ClassPathResource(RESOURCE_CONFIG).getInputStream()) {
                cachedConfig = normalize(objectMapper.readTree(inputStream));
                cachedMtime = -1;
                return cachedConfig;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Chatbot company config could not be loaded.", exception);
        }
    }

    public ConfigResponse publicConfig(String language) {
        ChatbotCompanyConfig config = loadConfig();
        String selectedLanguage = ChatbotText.resolveLanguage(language);
        JsonNode localized = config.locales().path(selectedLanguage);
        boolean english = "en".equals(selectedLanguage);
        Map<String, Object> theme = objectToMap(config.theme());
        theme.putAll(objectToMap(localized.path("theme")));
        theme.put("launcherLabel", text(localized, "launcherLabel", english ? "Open chat" : text(config.theme(), "launcherLabel", "Chat oeffnen")));
        Map<String, Object> handoff = objectToMap(config.handoff());
        handoff.putAll(objectToMap(localized.path("handoff")));
        handoff.put("label", text(localized, "handoffLabel", english ? "Request project" : text(config.handoff(), "label", "Kontakt aufnehmen")));
        return new ConfigResponse(
                text(localized, "botName", config.botName()),
                text(localized, "companyName", config.companyName()),
                text(localized, "subtitle", config.subtitle()),
                text(localized, "welcomeMessage", english ? "Hello! How can I help you with a website, backend system, automation or AI solution?" : config.welcomeMessage()),
                text(localized, "placeholder", english ? "Write your message..." : config.placeholder()),
                text(localized, "privacyNotice", english ? "Please do not send passwords, payment data, ID documents or private documents. Conversations may be stored for up to 7 days. The chatbot can make mistakes." : config.privacyNotice()),
                properties.maxMessageLength(),
                theme,
                handoff,
                Map.of(
                        "email", text(config.fallbackContact(), "email", ""),
                        "phone", text(config.fallbackContact(), "phone", ""),
                        "website", text(config.fallbackContact(), "website", "")
                )
        );
    }

    public String buildCompanyContext(ChatbotCompanyConfig config) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("companyKey", config.companyKey());
        root.put("companyName", config.companyName());
        root.put("botName", config.botName());
        root.set("handoff", config.handoff());
        root.set("fallbackContact", config.fallbackContact());
        root.put("pricing", config.pricing());
        root.set("rules", firstArrayItems(config.rules(), 30));
        root.set("leadQuestions", firstArrayItems(config.leadQuestions(), 20));
        JsonNode info = config.businessInfo();
        ObjectNode business = objectMapper.createObjectNode();
        copyText(info, business, "brand");
        copyText(info, business, "website");
        copyText(info, business, "shortDescription");
        copyText(info, business, "location");
        copyText(info, business, "teamLocation");
        business.set("services", compactObjects(info.path("services"), 14, List.of("name", "description")));
        business.set("targetCustomers", firstArrayItems(info.path("targetCustomers"), 10));
        business.set("benefits", firstArrayItems(info.path("benefits"), 10));
        business.set("process", compactObjects(info.path("process"), 6, List.of("step", "description")));
        business.set("technologies", firstArrayItems(info.path("technologies"), 10));
        business.set("referenceProjects", compactObjects(info.path("referenceProjects"), 6, List.of("name", "description", "url")));
        business.set("importantNotes", firstArrayItems(info.path("importantNotes"), 12));
        business.set("faq", compactObjects(info.path("faq"), 10, List.of("question", "answer")));
        if (info.path("contact").isObject()) business.set("contact", info.path("contact"));
        if (info.path("legal").isObject()) business.set("legal", info.path("legal"));
        root.set("businessInfo", business);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (IOException exception) {
            return root.toString();
        }
    }

    private ChatbotCompanyConfig normalize(JsonNode raw) {
        return new ChatbotCompanyConfig(
                text(raw, "botName", "Chat Assistant"),
                text(raw, "companyKey", slug(text(raw, "companyName", "company"))),
                text(raw, "companyName", "Company"),
                text(raw, "subtitle", ""),
                text(raw, "welcomeMessage", ""),
                text(raw, "placeholder", ""),
                text(raw, "privacyNotice", ""),
                object(raw.path("theme")),
                object(raw.path("fallbackContact")),
                object(raw.path("handoff")),
                object(raw.path("locales")),
                object(raw.path("businessInfo")),
                array(raw.path("rules")),
                array(raw.path("leadQuestions")),
                text(raw, "pricing", "")
        );
    }

    private Map<String, Object> objectToMap(JsonNode node) {
        Map<String, Object> output = new LinkedHashMap<>();
        if (node == null || !node.isObject()) return output;
        node.fields().forEachRemaining(entry -> output.put(entry.getKey(), entry.getValue().isTextual() ? entry.getValue().asText() : objectMapper.convertValue(entry.getValue(), Object.class)));
        return output;
    }

    private static String text(JsonNode node, String field, String fallback) {
        String value = node == null ? "" : node.path(field).asText("");
        return value == null || value.isBlank() ? fallback : ChatbotText.cleanInlineText(value, 800);
    }

    private static String slug(String value) {
        String slug = ChatbotText.cleanInlineText(value, 120)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "company" : slug;
    }

    private ObjectNode object(JsonNode node) {
        return node != null && node.isObject() ? (ObjectNode) node : objectMapper.createObjectNode();
    }

    private ArrayNode array(JsonNode node) {
        return node != null && node.isArray() ? (ArrayNode) node : objectMapper.createArrayNode();
    }

    private ArrayNode firstArrayItems(JsonNode node, int maxItems) {
        ArrayNode output = objectMapper.createArrayNode();
        if (node == null || !node.isArray()) return output;
        int count = 0;
        for (JsonNode item : node) {
            if (count >= maxItems) break;
            if (item.isTextual()) {
                output.add(ChatbotText.cleanInlineText(item.asText(), 360));
            } else {
                output.add(item);
            }
            count += 1;
        }
        return output;
    }

    private ArrayNode compactObjects(JsonNode node, int maxItems, List<String> fields) {
        ArrayNode output = objectMapper.createArrayNode();
        if (node == null || !node.isArray()) return output;
        int count = 0;
        for (JsonNode item : node) {
            if (count >= maxItems) break;
            ObjectNode compact = objectMapper.createObjectNode();
            for (String field : fields) copyText(item, compact, field);
            output.add(compact);
            count += 1;
        }
        return output;
    }

    private void copyText(JsonNode source, ObjectNode target, String field) {
        String value = source.path(field).asText("");
        if (!value.isBlank()) target.put(field, ChatbotText.cleanInlineText(value, 600));
    }
}
