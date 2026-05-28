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
        ServiceFixture fixture = fixture(
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

        ChatResponse completed = fixture.service().chat(
                null,
                "de",
                "Meine Website ist veraltet und soll erneuert werden. Kontakt: noelwenger@fabio.com"
        );

        assertTrue(completed.sessionEnded());
        assertTrue(fixture.sessionStore().find(completed.sessionId()).ended());

        ChatResponse next = fixture.service().chat(
                completed.sessionId(),
                "de",
                "Ich würde mich gerne über eine KI Automatisierung informieren"
        );

        assertFalse(next.sessionEnded());
        assertNotEquals(completed.sessionId(), next.sessionId());
        assertFalse(next.reply().contains("noelwenger@fabio.com"));
        assertFalse(next.reply().contains("Kontaktformular"));
    }

    @Test
    void vagueAutomationInquiryDoesNotAskForContactTooEarly() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Wie können wir Sie am besten kontaktieren?",
                        false,
                        "",
                        "",
                        "",
                        ""
                )
        );

        ChatResponse response = fixture.service().chat(
                null,
                "de",
                "Ich würde mich gerne über ein Projekt für KI Automatisierung informieren"
        );

        assertFalse(response.sessionEnded());
        assertTrue(response.reply().contains("Ablauf"));
        assertFalse(response.reply().contains("kontaktieren"));
        assertFalse(response.reply().contains("Kontaktformular"));
    }

    @Test
    void contactDetailsDoNotCompleteVagueAutomationInquiry() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        """
                        Danke, das ist eine klare Anfrage.

                        Ich kann Ihre Angaben nicht automatisch ins Kontaktformular eintragen oder ans Team senden.
                        Bitte senden Sie die Anfrage deshalb über das Kontaktformular:
                        https://bueno-ws.ch/contact

                        Für das Formular können Sie diese Angaben übernehmen:

                        Telefon
                        076 749 69 52

                        Gewünschte Lösung
                        KI-Automatisierung

                        Nachricht
                        Anfrage zu einem Projekt für KI-Automatisierung.
                        """,
                        true,
                        "",
                        "076 749 69 52",
                        "KI-Automatisierung",
                        "Anfrage zu einem Projekt für KI-Automatisierung."
                )
        );

        ChatResponse response = fixture.service().chat(
                null,
                "de",
                "Ich würde mich gerne über ein Projekt für KI Automatisierung informieren. Meine Telefonnummer ist 076 749 69 52"
        );

        assertFalse(response.sessionEnded());
        assertTrue(response.reply().contains("Ablauf"));
        assertFalse(response.reply().contains("Kontaktformular"));
        assertFalse(response.reply().contains("076 749 69 52"));
    }

    @Test
    void ambiguousChannelReplyAfterProjectQuestionIsClarified() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Welcher Ablauf soll als Erstes automatisiert werden?",
                        false,
                        "",
                        "",
                        "",
                        ""
                ),
                new AssistantMetadata(
                        "Welche spezifischen Funktionen oder Prozesse möchten Sie mit der KI-Automatisierung für SMS verbessern?",
                        false,
                        "",
                        "",
                        "",
                        ""
                )
        );

        ChatResponse first = fixture.service().chat(
                null,
                "de",
                "Ich würde mich gerne über ein Projekt für KI Automatisierung informieren"
        );

        ChatResponse second = fixture.service().chat(
                first.sessionId(),
                "de",
                "sms"
        );

        assertFalse(second.sessionEnded());
        assertTrue(second.reply().contains("SMS-Automatisierung"));
        assertTrue(second.reply().contains("Kontaktweg"));
    }

    @Test
    void phoneNumberAfterAmbiguousChannelStillRequiresProjectContext() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Welcher Ablauf soll als Erstes automatisiert werden?",
                        false,
                        "",
                        "",
                        "",
                        ""
                ),
                new AssistantMetadata(
                        "Welche spezifischen Funktionen oder Prozesse möchten Sie mit der KI-Automatisierung für SMS verbessern?",
                        false,
                        "",
                        "",
                        "",
                        ""
                ),
                new AssistantMetadata(
                        "Was genau möchten Sie mit der KI-Automatisierung für SMS erreichen?",
                        false,
                        "",
                        "076 749 69 52",
                        "",
                        ""
                )
        );

        ChatResponse first = fixture.service().chat(
                null,
                "de",
                "Ich würde mich gerne über ein Projekt für KI Automatisierung informieren"
        );
        ChatResponse second = fixture.service().chat(first.sessionId(), "de", "sms");
        ChatResponse third = fixture.service().chat(second.sessionId(), "de", "meine telefon nummer ist 076 749 69 52");

        assertFalse(third.sessionEnded());
        assertTrue(third.reply().contains("Ablauf"));
        assertFalse(third.reply().contains("SMS"));
        assertFalse(third.reply().contains("076 749 69 52"));
    }

    @Test
    void answeredExistingWebsiteQuestionIsNotRepeated() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Welche Aufgaben soll der KI-Chatbot auf der Website übernehmen?",
                        false,
                        "",
                        "",
                        "",
                        ""
                ),
                new AssistantMetadata(
                        "Gibt es bereits eine bestehende Website, die erneuert werden soll?",
                        false,
                        "",
                        "",
                        "",
                        ""
                ),
                new AssistantMetadata(
                        "Gibt es bereits eine bestehende Website, die erneuert werden soll?",
                        false,
                        "",
                        "",
                        "",
                        ""
                )
        );

        ChatResponse first = fixture.service().chat(
                null,
                "de",
                "Ich hätte gerne eine Webseite mit einem KI-Chatbot"
        );
        ChatResponse second = fixture.service().chat(first.sessionId(), "de", "momentan habe ich gar nichts");
        ChatResponse third = fixture.service().chat(second.sessionId(), "de", "nein");

        assertFalse(third.sessionEnded());
        assertTrue(third.reply().contains("KI-Chatbot"));
        assertFalse(third.reply().contains("bestehende Website"));
    }

    @Test
    void websiteChatbotInquiryDoesNotAskForCurrentToolOnFirstReply() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Was ist der aktuelle Ablauf oder das bestehende Tool, das Sie verwenden?",
                        false,
                        "",
                        "",
                        "",
                        ""
                )
        );

        ChatResponse response = fixture.service().chat(
                null,
                "de",
                "Ich hätte gerne eine Webseite mit einem KI-Chatbot"
        );

        assertFalse(response.reply().contains("aktueller Ablauf"));
        assertFalse(response.reply().contains("bestehende Tool"));
    }

    @Test
    void awkwardWebsiteChatbotQuestionIsReplaced() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Was soll der KI-Chatbot den Besuchern Ihrer Webseite helfen?",
                        false,
                        "",
                        "",
                        "",
                        ""
                )
        );

        ChatResponse response = fixture.service().chat(
                null,
                "de",
                "Ich hätte gerne eine Webseite mit einem KI-Chatbot"
        );

        assertTrue(response.reply().contains("Welche Aufgaben"));
        assertFalse(response.reply().contains("den Besuchern"));
    }

    private ServiceFixture fixture(AssistantMetadata... replies) {
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
        when(promptBuilder.repairInstructions(any(), any(), any(), any())).thenReturn("repair");
        when(openAiClient.classify(any(), any())).thenReturn(new Classification(ClassificationDecision.ANSWER, "project_lead", 0.95, "test"));
        int[] replyIndex = {0};
        when(openAiClient.createReply(any(), any())).thenAnswer(invocation -> replies[Math.min(replyIndex[0]++, replies.length - 1)]);
        when(openAiClient.repairReply(any(), any(), any(), any())).thenReturn(replies[replies.length - 1]);
        return new ServiceFixture(service, sessionStore);
    }

    private record ServiceFixture(ChatbotService service, ChatbotSessionStore sessionStore) {}

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
