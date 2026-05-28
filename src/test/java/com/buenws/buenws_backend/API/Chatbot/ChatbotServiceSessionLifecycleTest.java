package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.AssistantMetadata;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.Classification;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ClassificationDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatbotServiceSessionLifecycleTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void completedHandoffEndsSessionSoNextMessageStartsFresh() {
        ChatbotProperties properties = new ChatbotProperties(
                "sk-test",
                "gpt-test",
                "gpt-test",
                800,
                350,
                220,
                16,
                9000,
                360,
                7,
                "loopback",
                false,
                "inactive",
                10,
                60,
                10,
                10,
                60,
                ""
        );
        ChatbotCompanyConfig config = testConfig();
        ChatbotCompanyConfigService configService = mock(ChatbotCompanyConfigService.class);
        ChatbotSessionStore sessionStore = new ChatbotSessionStore(properties);
        OpenAiResponsesClient openAiClient = mock(OpenAiResponsesClient.class);
        PromptBuilder promptBuilder = mock(PromptBuilder.class);
        ChatbotConversationHistoryService historyService = mock(ChatbotConversationHistoryService.class);
        ChatbotService service = new ChatbotService(
                properties,
                configService,
                sessionStore,
                openAiClient,
                promptBuilder,
                new ContactExtractor(),
                historyService
        );

        when(configService.loadConfig()).thenReturn(config);
        when(configService.buildCompanyContext(config)).thenReturn("{}");
        when(promptBuilder.classificationInstructions(any(), any())).thenReturn("classify");
        when(promptBuilder.replyInstructions(any(), any(), any())).thenReturn("reply");
        when(openAiClient.classify(any(), any())).thenReturn(new Classification(ClassificationDecision.ANSWER, "project_lead", 0.95, "test"));
        when(openAiClient.createReply(any(), any())).thenReturn(
                new AssistantMetadata(
                        """
                        Danke, das ist eine klare Anfrage.

                        Ich kann Ihre Angaben nicht automatisch ins Kontaktformular eintragen oder ans Team senden.
                        Bitte senden Sie die Anfrage deshalb über das Kontaktformular:
                        https://bueno-ws.ch/contact

                        Für das Formular können Sie diese Angaben übernehmen:

                        E-Mail
                        noelwenger@fabio.com

                        Gewünschte Lösung
                        Website-Erneuerung

                        Nachricht
                        Die bestehende Website soll erneuert werden.
                        """,
                        true,
                        "noelwenger@fabio.com",
                        "",
                        "Website-Erneuerung",
                        "Die bestehende Website soll erneuert werden."
                ),
                new AssistantMetadata(
                        "Welcher Ablauf soll als Erstes automatisiert werden?",
                        false,
                        "",
                        "",
                        "",
                        ""
                )
        );

        ChatResponse completed = service.chat(
                null,
                "de",
                "Meine Website ist veraltet und soll erneuert werden. Kontakt: noelwenger@fabio.com"
        );

        assertTrue(completed.sessionEnded());
        assertTrue(sessionStore.find(completed.sessionId()).ended());

        ChatResponse next = service.chat(
                completed.sessionId(),
                "de",
                "Ich würde mich gerne über eine KI Automatisierung informieren"
        );

        assertFalse(next.sessionEnded());
        assertNotEquals(completed.sessionId(), next.sessionId());
        assertFalse(next.reply().contains("noelwenger@fabio.com"));
        assertFalse(next.reply().contains("Kontaktformular"));
    }

    private ChatbotCompanyConfig testConfig() {
        ObjectNode empty = objectMapper.createObjectNode();
        ObjectNode handoff = objectMapper.createObjectNode();
        handoff.put("url", "https://bueno-ws.ch/contact");
        return new ChatbotCompanyConfig(
                "Bueno Assistant",
                "bueno-ws",
                "Bueno Web Solutions",
                "",
                "",
                "",
                "",
                empty,
                empty,
                handoff,
                empty,
                empty,
                empty,
                empty,
                ""
        );
    }
}
