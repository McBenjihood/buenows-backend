package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.AssistantMetadata;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.Classification;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ClassificationDecision;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OpenAiResponsesClient {
    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");
    private final ChatbotProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiResponsesClient(ChatbotProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    public Classification classify(String instructions, Map<String, Object> payload) {
        JsonNode parsed = send(properties.openAiGuardModel(), List.of(message("developer", instructions), message("user", toJson(payload))), classificationFormat(), 0, properties.maxGuardOutputTokens(), "classification");
        ClassificationDecision decision;
        try {
            decision = ClassificationDecision.valueOf(parsed.path("decision").asText("clarify").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            decision = ClassificationDecision.CLARIFY;
        }
        return new Classification(decision, ChatbotText.cleanInlineText(parsed.path("category").asText("unclear_possible_business"), 120), ChatbotText.clamp(parsed.path("confidence").asDouble(0.5), 0, 1, 0.5), ChatbotText.cleanInlineText(parsed.path("reason").asText("No reason provided."), 500));
    }

    public AssistantMetadata createReply(String instructions, List<Map<String, String>> conversation) {
        List<Map<String, String>> input = new ArrayList<>();
        input.add(message("developer", instructions));
        input.addAll(conversation);
        return assistantMetadata(send(properties.openAiModel(), input, chatFormat(), 0.2, properties.maxOutputTokens(), "reply"));
    }

    public AssistantMetadata repairReply(String instructions, List<Map<String, String>> conversation, String issue, String draftReply) {
        List<Map<String, String>> input = new ArrayList<>();
        input.add(message("developer", instructions));
        input.addAll(conversation);
        input.add(message("user", toJson(Map.of("issue", issue, "draftReply", draftReply))));
        AssistantMetadata metadata = assistantMetadata(send(properties.openAiModel(), input, chatFormat(), 0.2, properties.maxOutputTokens(), "reply_repair"));
        return metadata.reply().isBlank() ? AssistantMetadata.empty(draftReply) : metadata;
    }

    public AssistantMetadata extractHandoffMetadata(String instructions, List<Map<String, String>> conversation, String draftReply) {
        List<Map<String, String>> input = new ArrayList<>();
        input.add(message("developer", instructions));
        input.addAll(conversation);
        input.add(message("user", toJson(Map.of("currentDraftReply", draftReply, "recoveryGoal", "Extract metadata for the contact-form template now."))));
        return assistantMetadata(send(properties.openAiModel(), input, chatFormat(), 0, Math.min(properties.maxOutputTokens(), 260), "handoff_metadata"));
    }

    private JsonNode send(String model, List<Map<String, String>> input, Map<String, Object> responseFormat, double temperature, int maxOutputTokens, String label) {
        if (!properties.openAiConfigured()) throw new IllegalStateException("OpenAI API key is missing.");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", input);
        body.put("text", Map.of("format", responseFormat));
        if (supportsTemperature(model)) {
            body.put("temperature", temperature);
        }
        if (supportsReasoning(model)) {
            body.put("reasoning", Map.of("effort", properties.openAiReasoningEffort()));
        }
        body.put("max_output_tokens", maxOutputTokens);
        body.put("store", false);
        for (int attempt = 1; attempt <= 2; attempt += 1) {
            try {
                HttpRequest request = HttpRequest.newBuilder(RESPONSES_URI)
                        .timeout(Duration.ofSeconds(90))
                        .header("Authorization", "Bearer " + properties.openAiApiKey())
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status >= 200 && status < 300) return ChatbotText.parseJsonObject(objectMapper, extractOutputText(response.body()));
                if (status == 401) throw new OpenAiClientException("OpenAI key is invalid.", status, label);
                if (attempt == 1 && isRetryable(status)) {
                    Thread.sleep(500);
                    continue;
                }
                throw new OpenAiClientException("OpenAI request failed.", status, label);
            } catch (IOException exception) {
                if (attempt == 2) throw new OpenAiClientException("OpenAI request failed.", 500, label, exception);
                try { Thread.sleep(500); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new OpenAiClientException("OpenAI request was interrupted.", 500, label, interrupted); }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new OpenAiClientException("OpenAI request was interrupted.", 500, label, exception);
            }
        }
        throw new OpenAiClientException("OpenAI request failed.", 500, label);
    }

    private String extractOutputText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        String outputText = root.path("output_text").asText("");
        if (!outputText.isBlank()) return outputText;
        StringBuilder text = new StringBuilder();
        for (JsonNode item : root.path("output")) for (JsonNode content : item.path("content")) {
            String itemText = content.path("text").asText("");
            if (!itemText.isBlank()) text.append(itemText);
        }
        if (text.isEmpty()) throw new IOException("OpenAI response did not contain output text.");
        return text.toString();
    }

    private AssistantMetadata assistantMetadata(JsonNode parsed) {
        return new AssistantMetadata(
                ChatbotText.cleanReply(parsed.path("reply").asText("")),
                parsed.path("readyForHandoff").asBoolean(false),
                ChatbotText.cleanInlineText(parsed.path("contactEmail").asText(""), 160),
                ChatbotText.cleanInlineText(parsed.path("desiredSolution").asText(""), 160),
                ChatbotText.cleanText(parsed.path("leadSummary").asText(""), 900),
                ChatbotText.cleanInlineText(parsed.path("nextAction").asText(""), 80),
                ChatbotText.cleanInlineText(parsed.path("missingField").asText(""), 80),
                parsed.path("projectContextComplete").asBoolean(false),
                parsed.path("contactRequired").asBoolean(false)
        );
    }

    private Map<String, Object> classificationFormat() {
        return Map.of("type", "json_schema", "name", "customer_input_classification", "strict", true, "schema", Map.of(
                "type", "object", "additionalProperties", false, "required", List.of("decision", "category", "confidence", "reason"),
                "properties", Map.of(
                        "decision", Map.of("type", "string", "enum", List.of("answer", "clarify", "reject")),
                        "category", Map.of("type", "string", "enum", List.of("business", "company_question", "project_lead", "unclear_possible_business", "off_topic", "abuse", "prompt_attack", "unsafe_request")),
                        "confidence", Map.of("type", "number"),
                        "reason", Map.of("type", "string")
                )));
    }

    private Map<String, Object> chatFormat() {
        return Map.of("type", "json_schema", "name", "customer_service_reply", "strict", true, "schema", Map.of(
                "type", "object", "additionalProperties", false, "required", List.of(
                        "reply",
                        "readyForHandoff",
                        "contactEmail",
                        "desiredSolution",
                        "leadSummary",
                        "nextAction",
                        "missingField",
                        "projectContextComplete",
                        "contactRequired"
                ),
                "properties", Map.of(
                        "reply", Map.of("type", "string"),
                        "readyForHandoff", Map.of("type", "boolean"),
                        "contactEmail", Map.of("type", "string"),
                        "desiredSolution", Map.of("type", "string"),
                        "leadSummary", Map.of("type", "string"),
                        "nextAction", Map.of("type", "string", "enum", List.of("answer", "ask_project_detail", "ask_email", "handoff", "human_contact", "decline")),
                        "missingField", Map.of("type", "string", "enum", List.of("none", "project_context", "current_process", "website_status", "chatbot_purpose", "ecommerce_detail", "timeframe", "email")),
                        "projectContextComplete", Map.of("type", "boolean"),
                        "contactRequired", Map.of("type", "boolean")
                )));
    }

    private Map<String, String> message(String role, String content) { return Map.of("role", role, "content", content); }
    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (IOException exception) { throw new IllegalStateException("Could not serialize OpenAI payload.", exception); }
    }
    private boolean isRetryable(int status) { return status == 408 || status == 409 || status == 429 || status >= 500; }
    private boolean supportsTemperature(String model) {
        String normalized = model == null ? "" : model.toLowerCase(Locale.ROOT);
        return !supportsReasoning(normalized);
    }
    private boolean supportsReasoning(String model) {
        String normalized = model == null ? "" : model.toLowerCase(Locale.ROOT);
        return normalized.startsWith("gpt-5") || normalized.startsWith("o");
    }

    public static class OpenAiClientException extends RuntimeException {
        private final int status;
        private final String label;
        public OpenAiClientException(String message, int status, String label) { super(message); this.status = status; this.label = label; }
        public OpenAiClientException(String message, int status, String label, Throwable cause) { super(message, cause); this.status = status; this.label = label; }
        public int status() { return status; }
        public String label() { return label; }
    }
}
