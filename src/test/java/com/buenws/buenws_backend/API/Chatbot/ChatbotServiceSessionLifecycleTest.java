package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.AssistantMetadata;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.Classification;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ClassificationDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatbotServiceSessionLifecycleTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void completedHandoffEndsSessionSoNextMessageStartsFresh() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        """
                        Danke, das ist eine klare Anfrage.

                        Ich kann die Anfrage nicht automatisch absenden. Ich habe die Angaben für das Kontaktformular vorbereitet.
                        Bitte öffnen Sie das Kontaktformular, prüfen Sie die Felder und senden Sie die Anfrage ab:
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
        assertNotNull(completed.handoffDraft());
        assertEquals("noelwenger@fabio.com", completed.handoffDraft().email());
        assertEquals("Website-Erneuerung", completed.handoffDraft().title());
        assertEquals("http://localhost:5173/contact", completed.handoffDraft().contactUrl());
        assertTrue(completed.handoffDraft().message().contains("bestehende Website"));
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
        assertNull(response.handoffDraft());
        assertTrue(response.reply().contains("Ablauf"), response.reply());
        assertFalse(response.reply().contains("kontaktieren"));
        assertFalse(response.reply().contains("Kontaktformular"));
    }

    @Test
    void phoneOnlyContactDoesNotCompleteAndAsksForEmail() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        """
                        Danke, das ist eine klare Anfrage.

                        Ich kann die Anfrage nicht automatisch absenden. Ich habe die Angaben für das Kontaktformular vorbereitet.
                        Bitte öffnen Sie das Kontaktformular, prüfen Sie die Felder und senden Sie die Anfrage ab:
                        https://bueno-ws.ch/contact

                        Für das Formular können Sie diese Angaben übernehmen:

                        Telefon
                        076 749 69 52

                        Gewünschte Lösung
                        Website mit Terminbuchung

                        Nachricht
                        Neue Website für ein lokales Geschäft mit Terminbuchung.
                        """,
                        true,
                        "",
                        "076 749 69 52",
                        "Website mit Terminbuchung",
                        "Neue Website für ein lokales Geschäft mit Terminbuchung."
                )
        );

        ChatResponse response = fixture.service().chat(
                null,
                "de",
                "Ich brauche eine neue Website für mein lokales Geschäft mit Terminbuchung. Meine Telefonnummer ist 076 749 69 52"
        );

        assertFalse(response.sessionEnded());
        assertNull(response.handoffDraft());
        assertTrue(response.reply().contains("E-Mail-Adresse"), response.reply());
        assertFalse(response.reply().contains("Kontaktformular"));
        assertFalse(response.reply().contains("076 749 69 52"));
    }

    @Test
    void generalContactQuestionAfterConcreteProjectAsksForEmailOnly() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Wie dürfen wir Sie erreichen?",
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
                "Ich brauche einen Online-Shop, in dem Kunden Schuhe ansehen und direkt mit Karte oder PayPal kaufen können"
        );

        assertFalse(response.sessionEnded());
        assertNull(response.handoffDraft());
        assertTrue(response.reply().contains("E-Mail-Adresse"), response.reply());
        assertFalse(response.reply().contains("Telefon"));
        assertFalse(response.reply().contains("Kontaktmöglichkeit"));
    }

    @Test
    void smsOrPhonePreferenceAfterProjectStillAsksForEmail() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Wie lautet Ihre E-Mail-Adresse?",
                        false,
                        "",
                        "",
                        "",
                        ""
                ),
                new AssistantMetadata(
                        "Wie lautet die Telefonnummer, unter der wir Sie erreichen können?",
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
                "Ich brauche einen Online-Shop, in dem Kunden etwa 100 Schuhmodelle ansehen und direkt mit Karte oder PayPal kaufen können"
        );
        ChatResponse second = fixture.service().chat(first.sessionId(), "de", "per sms");

        assertFalse(second.sessionEnded());
        assertNull(second.handoffDraft());
        assertTrue(second.reply().contains("E-Mail-Adresse"), second.reply());
        assertTrue(second.reply().contains("Projektanfragen"), second.reply());
        assertFalse(second.reply().contains("Telefonnummer"));
    }

    @Test
    void contactDetailsDoNotCompleteVagueAutomationInquiry() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        """
                        Danke, das ist eine klare Anfrage.

                        Ich kann die Anfrage nicht automatisch absenden. Ich habe die Angaben für das Kontaktformular vorbereitet.
                        Bitte öffnen Sie das Kontaktformular, prüfen Sie die Felder und senden Sie die Anfrage ab:
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
        assertTrue(response.reply().contains("Ablauf"), response.reply());
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
                        "Was soll der Chatbot den Besuchern Ihrer Webseite helfen?",
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

    @Test
    void websiteChatbotPurposeIsNotAskedAgainAfterUserAnsweredIt() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Welche Aufgaben soll der KI-Chatbot auf der Website übernehmen?",
                        false,
                        "",
                        "",
                        "",
                        "ask_project_detail",
                        "chatbot_purpose",
                        false,
                        false
                ),
                new AssistantMetadata(
                        "Gibt es bereits eine bestehende Webseite oder ein aktuelles System, das Sie nutzen?",
                        false,
                        "",
                        "",
                        "",
                        "ask_project_detail",
                        "website_status",
                        false,
                        false
                ),
                new AssistantMetadata(
                        "Welche Aufgaben soll der KI-Chatbot auf der Website übernehmen?",
                        false,
                        "",
                        "",
                        "",
                        "ask_project_detail",
                        "chatbot_purpose",
                        false,
                        false
                )
        );

        ChatResponse first = fixture.service().chat(null, "de", "Ich hätte gerne eine Webseite mit Chatbot");
        ChatResponse second = fixture.service().chat(first.sessionId(), "de", "Terminvereinbarung und Kundenservice");
        ChatResponse third = fixture.service().chat(second.sessionId(), "de", "nein");

        assertFalse(third.sessionEnded());
        assertTrue(third.reply().contains("E-Mail-Adresse"), third.reply());
        assertFalse(third.reply().contains("Aufgaben soll der KI-Chatbot"), third.reply());
        assertFalse(third.reply().contains("bestehende Webseite"), third.reply());
    }

    @Test
    void englishWebsiteChatbotPurposeIsNotAskedAgainAfterUserAnsweredIt() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "What tasks should the AI chatbot handle on the website?",
                        false,
                        "",
                        "",
                        "",
                        "ask_project_detail",
                        "chatbot_purpose",
                        false,
                        false
                ),
                new AssistantMetadata(
                        "Is there already an existing website or current system?",
                        false,
                        "",
                        "",
                        "",
                        "ask_project_detail",
                        "website_status",
                        false,
                        false
                ),
                new AssistantMetadata(
                        "What tasks should the AI chatbot handle on the website?",
                        false,
                        "",
                        "",
                        "",
                        "ask_project_detail",
                        "chatbot_purpose",
                        false,
                        false
                )
        );

        ChatResponse first = fixture.service().chat(null, "en", "I would like a website with a chatbot");
        ChatResponse second = fixture.service().chat(first.sessionId(), "en", "appointment booking and customer service");
        ChatResponse third = fixture.service().chat(second.sessionId(), "en", "no");

        assertFalse(third.sessionEnded());
        assertTrue(third.reply().contains("email address"), third.reply());
        assertFalse(third.reply().toLowerCase().contains("what tasks should the ai chatbot"), third.reply());
        assertFalse(third.reply().toLowerCase().contains("existing website"), third.reply());
    }

    @Test
    void ecommerceFunctionQuestionIsNotRepeatedAfterUserAnsweredIt() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Gibt es bereits eine bestehende Website, die erneuert werden soll?",
                        false,
                        "",
                        "",
                        "",
                        ""
                ),
                new AssistantMetadata(
                        "Welche Inhalte oder Funktionen soll die neue Website haben?",
                        false,
                        "",
                        "",
                        "",
                        ""
                ),
                new AssistantMetadata(
                        "Welche Inhalte oder Funktionen soll die neue Website haben?",
                        false,
                        "",
                        "",
                        "",
                        ""
                )
        );

        ChatResponse first = fixture.service().chat(null, "de", "Ich würde gerne eine Webseite kaufen, über die ich Schuhe verkaufen kann");
        ChatResponse second = fixture.service().chat(first.sessionId(), "de", "nein");
        ChatResponse third = fixture.service().chat(second.sessionId(), "de", "Ich will Schuhe zeigen können und Kunden können die dann kaufen");

        assertFalse(third.sessionEnded());
        assertTrue(third.reply().contains("Zahlungsarten"));
        assertFalse(third.reply().contains("Inhalte oder Funktionen"));
    }

    @Test
    void humanContactRequestDoesNotClaimForwarding() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Ich leite Sie gerne an eine echte Person weiter. Wie kann ich Ihnen konkret helfen?",
                        false,
                        "",
                        "",
                        "",
                        ""
                )
        );

        ChatResponse response = fixture.service().chat(null, "de", "Kann ich mit einer echten Person reden?");

        assertFalse(response.sessionEnded());
        assertTrue(response.reply().contains("info.buenows@gmail.com"), response.reply());
        assertFalse(response.reply().contains("+41"), response.reply());
        assertFalse(response.reply().contains("leite"));
    }

    @Test
    void humanContactPhoneReplyIsReplacedWithEmailOnlyContact() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Sie können Bueno Web Solutions telefonisch unter +41 077 523 88 36 kontaktieren.",
                        false,
                        "",
                        "",
                        "",
                        ""
                )
        );

        ChatResponse response = fixture.service().chat(null, "de", "Wie kann ich eine echte Person kontaktieren?");

        assertFalse(response.sessionEnded());
        assertTrue(response.reply().contains("info.buenows@gmail.com"), response.reply());
        assertFalse(response.reply().contains("+41"), response.reply());
        assertFalse(response.reply().contains("telefonisch"));
    }

    @Test
    void sensitiveDataIsNotStoredOrSentToOpenAi() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata("ignored", false, "", "", "", "")
        );

        ChatResponse response = fixture.service().chat(null, "de", "Mein Passwort ist SuperSecret123");

        assertFalse(response.sessionEnded());
        assertTrue(response.reply().contains("Passwörter"));
        verify(fixture.historyService(), never()).saveUserMessage(any(), any(), any());
        verify(fixture.openAiClient(), never()).classify(any(), any());
    }

    @Test
    void websiteChatbotDoesNotCompleteBeforeChatbotPurposeIsKnown() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        """
                        Danke, das ist eine klare Anfrage.

                        Ich kann die Anfrage nicht automatisch absenden. Ich habe die Angaben für das Kontaktformular vorbereitet.
                        Bitte öffnen Sie das Kontaktformular, prüfen Sie die Felder und senden Sie die Anfrage ab:
                        https://bueno-ws.ch/contact

                        E-Mail
                        test@example.com

                        Gewünschte Lösung
                        Website mit KI-Chatbot

                        Nachricht
                        Neue Website mit KI-Chatbot.
                        """,
                        true,
                        "test@example.com",
                        "Website mit KI-Chatbot",
                        "Neue Website mit KI-Chatbot.",
                        "handoff",
                        "none",
                        true,
                        false
                )
        );

        ChatResponse response = fixture.service().chat(
                null,
                "de",
                "Ich hätte gerne eine Webseite mit einem KI-Chatbot. Meine E-Mail ist test@example.com"
        );

        assertFalse(response.sessionEnded());
        assertNull(response.handoffDraft());
        assertTrue(response.reply().contains("KI-Chatbot"), response.reply());
        assertTrue(response.reply().contains("Aufgaben"), response.reply());
        assertFalse(response.reply().contains("Kontaktformular"));
    }

    @Test
    void ecommerceDoesNotCompleteBeforeUsefulShopDetailIsKnown() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        """
                        Danke, das ist eine klare Anfrage.

                        Ich kann die Anfrage nicht automatisch absenden. Ich habe die Angaben für das Kontaktformular vorbereitet.
                        Bitte öffnen Sie das Kontaktformular, prüfen Sie die Felder und senden Sie die Anfrage ab:
                        https://bueno-ws.ch/contact

                        E-Mail
                        shop@example.com

                        Gewünschte Lösung
                        Online-Shop

                        Nachricht
                        Online-Shop für Schuhe.
                        """,
                        true,
                        "shop@example.com",
                        "Online-Shop",
                        "Online-Shop für Schuhe.",
                        "handoff",
                        "none",
                        true,
                        false
                )
        );

        ChatResponse response = fixture.service().chat(
                null,
                "de",
                "Ich möchte eine Webseite, über die ich Schuhe verkaufen kann. Meine E-Mail ist shop@example.com"
        );

        assertFalse(response.sessionEnded());
        assertNull(response.handoffDraft());
        assertTrue(response.reply().contains("Zahlungsarten"), response.reply());
        assertFalse(response.reply().contains("Kontaktformular"));
    }

    @Test
    void imperativeEmailRequestIsCaughtBeforeProjectContextIsClear() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Bitte teilen Sie mir Ihre E-Mail-Adresse mit.",
                        false,
                        "",
                        "",
                        "",
                        "ask_email",
                        "email",
                        false,
                        true
                )
        );

        ChatResponse response = fixture.service().chat(
                null,
                "de",
                "Ich würde mich gerne über KI-Automatisierung informieren"
        );

        assertFalse(response.sessionEnded());
        assertNull(response.handoffDraft());
        assertTrue(response.reply().contains("Ablauf"), response.reply());
        assertFalse(response.reply().contains("E-Mail-Adresse"));
    }

    @Test
    void unsafeGuaranteeIsRemovedFromFinalReply() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Diese Website wird garantiert mehr Kunden bringen.",
                        false,
                        "",
                        "",
                        "",
                        "answer",
                        "none",
                        false,
                        false
                )
        );

        ChatResponse response = fixture.service().chat(
                null,
                "de",
                "Ich möchte eine neue Website"
        );

        assertFalse(response.sessionEnded());
        assertFalse(response.reply().toLowerCase().contains("garantiert"), response.reply());
        assertFalse(response.reply().toLowerCase().contains("sicher mehr"), response.reply());
    }

    @Test
    void invoiceEmailDeliveryIsNotTreatedAsContactChannel() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Wie versenden Sie derzeit Rechnungen an Ihre Kunden?",
                        false,
                        "",
                        "",
                        "",
                        "ask_project_detail",
                        "current_process",
                        false,
                        false
                ),
                new AssistantMetadata(
                        "Welche Funktionen sind Ihnen bei der Automatisierung wichtig?",
                        false,
                        "",
                        "",
                        "",
                        "ask_project_detail",
                        "project_context",
                        false,
                        false
                ),
                new AssistantMetadata(
                        "Meinen Sie eine E-Mail-Automatisierung oder E-Mail nur als Kontaktweg?",
                        false,
                        "",
                        "",
                        "",
                        "ask_project_detail",
                        "project_context",
                        false,
                        false
                )
        );

        ChatResponse first = fixture.service().chat(
                null,
                "de",
                "Ich hätte gerne ein Tool für die Automatisierung vom Versenden von Rechnungen an Kunden"
        );
        ChatResponse second = fixture.service().chat(first.sessionId(), "de", "Manuell. Sie werden ausgedruckt und versendet.");
        ChatResponse third = fixture.service().chat(second.sessionId(), "de", "Es soll die Rechnungen per E-Mail versenden.");

        assertFalse(third.sessionEnded());
        assertNull(third.handoffDraft());
        assertTrue(third.reply().contains("E-Mail-Adresse"), third.reply());
        assertFalse(third.reply().contains("Kontaktweg"), third.reply());
        assertFalse(third.reply().contains("Meinen Sie"), third.reply());
    }

    @Test
    void unscopedBookingAndSupportDoesNotAskForEmailTooEarly() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        "Wie lautet Ihre E-Mail-Adresse?",
                        false,
                        "",
                        "",
                        "",
                        "ask_email",
                        "email",
                        false,
                        true
                )
        );

        ChatResponse response = fixture.service().chat(null, "de", "Terminvereinbarung und Kundenservice");

        assertFalse(response.sessionEnded());
        assertNull(response.handoffDraft());
        assertTrue(response.reply().contains("Website"), response.reply());
        assertTrue(response.reply().contains("KI-Chatbot"), response.reply());
        assertTrue(response.reply().contains("Automatisierungstool"), response.reply());
        assertFalse(response.reply().contains("E-Mail-Adresse"), response.reply());
    }

    @Test
    void unsupportedHandoffScopeFromAiMetadataIsIgnored() {
        ServiceFixture fixture = fixture(
                new AssistantMetadata(
                        """
                        Danke, das ist eine klare Anfrage.

                        Ich kann die Anfrage nicht automatisch absenden. Ich habe die Angaben für das Kontaktformular vorbereitet. Bitte öffnen Sie das Kontaktformular, prüfen Sie die Felder und senden Sie die Anfrage ab:
                        https://bueno-ws.ch/contact

                        E-Mail
                        client@example.com

                        Gewünschte Lösung
                        Website mit KI-Chatbot

                        Nachricht
                        Der Nutzer möchte eine Website mit einem KI-Chatbot.
                        """,
                        true,
                        "client@example.com",
                        "Website mit KI-Chatbot",
                        "Der Nutzer möchte eine Website mit einem KI-Chatbot.",
                        "handoff",
                        "none",
                        true,
                        false
                )
        );

        ChatResponse response = fixture.service().chat(
                null,
                "de",
                "Ich brauche eine Automatisierung, die Rechnungen automatisch erstellt und per E-Mail an Kunden sendet. Meine E-Mail ist client@example.com"
        );

        assertTrue(response.sessionEnded());
        assertNotNull(response.handoffDraft());
        assertTrue(response.handoffDraft().title().contains("Rechnungsautomatisierung"), response.handoffDraft().title());
        assertFalse(response.handoffDraft().title().contains("Chatbot"), response.handoffDraft().title());
        assertFalse(response.handoffDraft().message().contains("Chatbot"), response.handoffDraft().message());
        assertTrue(response.handoffDraft().message().contains("Rechnungen"), response.handoffDraft().message());
    }

    private ServiceFixture fixture(AssistantMetadata... replies) {
        ChatbotProperties properties = new ChatbotProperties(
                "sk-test",
                "gpt-test",
                "gpt-test",
                "low",
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
                "",
                "http://localhost:5173/contact"
        );
        ChatbotCompanyConfig config = testConfig();
        ChatbotCompanyConfigService configService = mock(ChatbotCompanyConfigService.class);
        ChatbotSessionStore sessionStore = new ChatbotSessionStore(properties);
        OpenAiResponsesClient openAiClient = mock(OpenAiResponsesClient.class);
        PromptBuilder promptBuilder = mock(PromptBuilder.class);
        ChatbotConversationHistoryService historyService = mock(ChatbotConversationHistoryService.class);
        ContactExtractor contactExtractor = new ContactExtractor();
        ProjectContextEvaluator projectContext = new ProjectContextEvaluator(contactExtractor);
        LanguageSafetyGuard languageSafety = new LanguageSafetyGuard(projectContext);
        ReplyQualityGuard replyQuality = new ReplyQualityGuard(contactExtractor, projectContext, languageSafety);
        HandoffRenderer handoffRenderer = new HandoffRenderer(properties, contactExtractor, projectContext, replyQuality, languageSafety);
        SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
        ChatbotService service = new ChatbotService(
                properties,
                configService,
                sessionStore,
                openAiClient,
                promptBuilder,
                contactExtractor,
                historyService,
                projectContext,
                replyQuality,
                handoffRenderer,
                languageSafety,
                sensitiveDataGuard
        );

        when(configService.loadConfig()).thenReturn(config);
        when(configService.buildCompanyContext(config)).thenReturn("{}");
        when(promptBuilder.classificationInstructions(any(), any())).thenReturn("classify");
        when(promptBuilder.replyInstructions(any(), any(), any())).thenReturn("reply");
        when(promptBuilder.repairInstructions(any(), any(), any(), any())).thenReturn("repair");
        when(promptBuilder.handoffMetadataInstructions(any(), any())).thenReturn("handoff");
        when(historyService.restoreActiveSession(any())).thenReturn(Optional.empty());
        when(openAiClient.classify(any(), any())).thenReturn(new Classification(ClassificationDecision.ANSWER, "project_lead", 0.95, "test"));
        int[] replyIndex = {0};
        when(openAiClient.createReply(any(), any())).thenAnswer(invocation -> replies[Math.min(replyIndex[0]++, replies.length - 1)]);
        when(openAiClient.repairReply(any(), any(), any(), any())).thenReturn(replies[replies.length - 1]);
        return new ServiceFixture(service, sessionStore, openAiClient, historyService);
    }

    private record ServiceFixture(ChatbotService service, ChatbotSessionStore sessionStore,
                                  OpenAiResponsesClient openAiClient,
                                  ChatbotConversationHistoryService historyService) {}

    private ChatbotCompanyConfig testConfig() {
        ObjectNode empty = objectMapper.createObjectNode();
        ObjectNode fallbackContact = objectMapper.createObjectNode();
        fallbackContact.put("email", "info.buenows@gmail.com");
        fallbackContact.put("phone", "+41 077 523 88 36");
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
                fallbackContact,
                handoff,
                empty,
                empty,
                empty,
                empty,
                ""
        );
    }
}
