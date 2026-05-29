package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.AssistantMetadata;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatSession;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ReplyQualityGuard {
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\b(Gewuenschte Loesung|Gewünschte Lösung|Desired solution|Nachricht|Message)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTACT_ASK_PATTERN = Pattern.compile("\\b(best contact|best way to contact|contact you|contact details|reach you|get in touch|email address|phone number|Kontaktmoeglichkeit|Kontaktmöglichkeit|Kontaktdaten|kontaktieren|erreichen|melden|E-Mail-Adresse|Telefonnummer|per Mail melden)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTACT_REQUEST_PATTERN = Pattern.compile("\\b(please|kindly|send|provide|enter|share|tell us|bitte|teilen Sie|geben Sie|nennen Sie|senden Sie|schicken Sie|hinterlassen Sie|ich brauche|wir brauchen)\\b.{0,90}\\b(email|e-mail|email address|contact details|phone number|E-Mail|E-Mail-Adresse|Mailadresse|Kontaktdaten|Kontaktmöglichkeit|Telefonnummer)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern BAD_FILLER_PATTERN = Pattern.compile("\\b(Das klingt|Es klingt|Danke fuer die Informationen|Danke für die Informationen|Es waere hilfreich|Es wäre hilfreich|Ich benoetige noch|Ich benötige noch|Moechten Sie|Möchten Sie|Koennten Sie|Könnten Sie|That sounds|It sounds|To clarify|This could involve|It would be helpful|Would you like to|Could you please|we can develop|we could develop)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE_PROMISE_PATTERN = Pattern.compile("\\b(we guarantee|we promise|guaranteed results|guaranteed leads|guaranteed customers|will increase leads|will generate customers|100%|definitely improve|wir garantieren|wir versprechen|garantierte kunden|garantierte anfragen|garantiert mehr|definitiv mehr|100 ?%|sicher mehr kunden|sicher mehr anfragen|wird mehr kunden bringen|wird mehr anfragen bringen)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern THANK_YOU_INQUIRY_PATTERN = Pattern.compile("\\b(Vielen Dank fuer Ihre Anfrage|Vielen Dank für Ihre Anfrage|Vielen Dank f\\u00C3\\u00BCr Ihre Anfrage|Thank you for your inquiry)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEAK_QUESTION_PATTERN = Pattern.compile("\\b(Could you|K.nnten)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[[^\\]]+]\\(https?://[^)]+\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EARLY_HANDOFF_PATTERN = Pattern.compile("\\b(contact form|contact page|project request|Projekt anfragen|Kontaktformular|Kontaktseite|Formular|Anfrageformular|Handoff)\\b|https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTACT_PREFERENCE_PATTERN = Pattern.compile("\\b(sms|text message|telefon|phone|call|anruf|whatsapp|e-mail|email|mail)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CURRENT_PROCESS_TOOL_ASK_PATTERN = Pattern.compile("\\b(aktuelle?r? Ablauf|bestehende?s Tool|current process|current tool|software|tools?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AWKWARD_CHATBOT_TASK_ASK_PATTERN = Pattern.compile("\\bWas soll der (?:KI-)?Chatbot\\b.{0,140}\\bhelfen\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALSE_FORWARDING_CLAIM_PATTERN = Pattern.compile("\\b(ich leite|wir leiten|I will forward|I'll forward|I can forward|weiterleiten|weitergeleitet|team kontaktieren|Termin direkt vereinbaren|schedule.*for you)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIRECT_CONTACT_PATTERN = Pattern.compile("\\b(info\\.buenows@gmail\\.com|bueno-ws\\.ch/contact|kontaktformular|contact form|e-mail|email)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_CONTACT_OUTPUT_PATTERN = Pattern.compile("\\b(\\+41|telefonisch|telefon|phone|call)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHANNEL_CLARIFICATION_PATTERN = Pattern.compile("\\b(E-Mail-Automatisierung oder E-Mail nur als Kontaktweg|SMS-Automatisierung oder SMS nur als Kontaktweg|WhatsApp-Automatisierung oder WhatsApp nur als Kontaktweg|Telefonassistenten oder Telefon nur als Kontaktweg|email automation or email only as the contact channel|SMS automation or SMS only as the contact channel|WhatsApp automation or WhatsApp only as the contact channel|phone assistant or phone only as the contact channel)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXISTING_WEBSITE_QUESTION_PATTERN = Pattern.compile("\\b(bestehende Website|bestehende Webseite|existing website|redesigned)\\b", Pattern.CASE_INSENSITIVE);

    private final ContactExtractor contactExtractor;
    private final ProjectContextEvaluator projectContext;
    private final LanguageSafetyGuard languageSafety;

    public ReplyQualityGuard(ContactExtractor contactExtractor, ProjectContextEvaluator projectContext, LanguageSafetyGuard languageSafety) {
        this.contactExtractor = contactExtractor;
        this.projectContext = projectContext;
        this.languageSafety = languageSafety;
    }

    public String getReplyQualityIssue(String reply, ChatSession session, AssistantMetadata metadata, String language) {
        if (languageSafety.replyLooksWrongLanguage(reply, language)) return "The reply is not in the fixed session language.";
        if (MARKDOWN_LINK_PATTERN.matcher(reply == null ? "" : reply).find()) return "The reply uses a Markdown link. Use plain text only. Do not link to the contact form before final handoff.";
        if (needsEarlyProjectFallback(reply, session)) return "The reply redirects to the contact form too early. Continue the conversation by asking one practical follow-up question in chat.";
        if (metadata.readyForHandoff() && !projectContext.hasEnoughProjectContextForHandoff(session)) return "The metadata marks the lead as ready too early. Ask the next useful project detail first.";
        if (metadataRequestsContactTooEarly(metadata, session)) return "The metadata asks for contact too early. Ask what process, task, shop detail or chatbot purpose is still missing first.";
        if (asksForContactOption(reply) && !projectContext.hasEnoughProjectContextForHandoff(session)) return "The reply asks for contact information before the project need is clear. Ask what process, task or current workflow should be improved first.";
        if (asksForContactOption(reply) && !contactExtractor.conversationHasContactInfo(session.messages())) return "Ask specifically for the user's email address. Do not ask generally for contact options and do not ask for phone, SMS, WhatsApp or calls as the project request contact.";
        if (projectContext.repeatsPreviousAssistantQuestion(reply, session)) return "The reply repeats the previous assistant question after the user answered it. Ask the next useful project question instead.";
        if (asksWrongCurrentToolQuestionForWebsiteChatbot(reply, session)) return "The user wants a website with an AI chatbot. Ask what the chatbot should do on the website instead of asking for a current process or tool.";
        if (asksAwkwardChatbotTaskQuestion(reply)) return "The German question is grammatically awkward. Ask: Welche Aufgaben soll der KI-Chatbot auf der Website übernehmen?";
        if (asksKnownChatbotPurpose(reply, session)) return "The user already explained what the chatbot should do. Do not ask for the chatbot purpose again.";
        if (asksKnownWebsiteOrSystemStatus(reply, session)) return "The user already answered whether there is an existing website or system. Do not ask that again.";
        if (asksWrongChannelClarificationForWorkflow(reply, session)) return "The user mentioned the channel as part of the workflow to automate, not as a contact channel. Continue the project flow instead of asking a channel-clarification question.";
        if (asksSpecificScopeForUnscopedFunctions(reply, session)) return "The user only named desired functions such as appointment booking or customer service. First ask whether this should be a website, chatbot, backend system or automation tool.";
        if (projectContext.lastUserRequestsHumanContact(session) && containsFalseForwardingClaim(reply)) return "The user asks for human contact. Do not claim that the team is contacted or that anything is forwarded. Provide the public email/contact form plainly.";
        if (metadata.readyForHandoff() && !replyLooksLikeTemplate(reply)) return "The metadata says handoff is ready but the reply does not use the exact contact-form template.";
        if (contactExtractor.conversationHasContactInfo(session.messages()) && asksForContactOption(reply)) return "The user already provided an email address. Generate the contact-form template if the project is concrete enough.";
        if (lastMessageHasContactAndLanguageSwitch(session) && !replyLooksLikeTemplate(reply)) return "The user asked to switch language and provided contact information. Keep the fixed language and generate the template if context is clear.";
        if (BAD_FILLER_PATTERN.matcher(reply == null ? "" : reply).find() || THANK_YOU_INQUIRY_PATTERN.matcher(reply == null ? "" : reply).find() || WEAK_QUESTION_PATTERN.matcher(reply == null ? "" : reply).find()) return "The reply uses filler or weak customer-service phrasing. Ask directly without 'Could you' or 'Könnten Sie'.";
        if (UNSAFE_PROMISE_PATTERN.matcher(reply == null ? "" : reply).find()) return "The reply makes a promise or guarantee.";
        return null;
    }

    public boolean needsStructuredHandoffRecovery(ChatSession session, String reply) {
        return projectContext.hasEnoughProjectContextForHandoff(session)
                && contactExtractor.conversationHasContactInfo(session.messages())
                && !replyLooksLikeTemplate(reply)
                && (lastMessageHasContactAndLanguageSwitch(session) || asksForContactOption(reply) || asksOptionalDetailAfterContact(session, reply));
    }

    public boolean needsProjectFollowUpFallback(String reply, ChatSession session) {
        return needsEarlyProjectFallback(reply, session)
                || needsWeakProjectQuestionFallback(reply, session)
                || asksWrongCurrentToolQuestionForWebsiteChatbot(reply, session)
                || asksAwkwardChatbotTaskQuestion(reply)
                || asksKnownChatbotPurpose(reply, session)
                || asksKnownWebsiteOrSystemStatus(reply, session)
                || asksSpecificScopeForUnscopedFunctions(reply, session);
    }

    public boolean shouldUseHumanContactFallback(ChatSession session, String reply) {
        if (!projectContext.lastUserRequestsHumanContact(session)) return false;
        String value = reply == null ? "" : reply;
        return containsFalseForwardingClaim(value)
                || PHONE_CONTACT_OUTPUT_PATTERN.matcher(value).find()
                || (!DIRECT_CONTACT_PATTERN.matcher(value).find() && value.contains("?"));
    }

    public boolean needsContactDelayFallback(String reply, ChatSession session) {
        return asksForContactOption(reply) && !projectContext.hasEnoughProjectContextForHandoff(session);
    }

    public boolean lastUserContactButProjectContextMissing(ChatSession session) {
        return contactExtractor.textHasContactLikeValue(projectContext.getLastUserMessage(session))
                && !projectContext.hasEnoughProjectContextForHandoff(session);
    }

    public boolean replyLooksLikeTemplate(String reply) {
        return TEMPLATE_PATTERN.matcher(reply == null ? "" : reply).find();
    }

    public boolean asksForContactOption(String reply) {
        String value = reply == null ? "" : reply;
        return CONTACT_ASK_PATTERN.matcher(value).find() && value.contains("?")
                || CONTACT_REQUEST_PATTERN.matcher(value).find();
    }

    public boolean isAmbiguousChannelReply(ChatSession session) {
        String lastUser = projectContext.getLastUserMessage(session);
        String lastQuestion = projectContext.getLastAssistantQuestion(session);
        return projectContext.getUserMessageCount(session) > 1
                && projectContext.isContactPreferenceWithoutConcreteInfo(lastUser)
                && !lastQuestion.isBlank()
                && !asksForContactOption(lastQuestion);
    }

    public boolean lastUserMessageIsContactPreference(ChatSession session) {
        return projectContext.isContactPreferenceWithoutConcreteInfo(projectContext.getLastUserMessage(session));
    }

    public String buildProjectFollowUpFallback(ChatSession session, String language) {
        String text = projectContext.userConversationText(session).toLowerCase(Locale.ROOT);
        if (projectContext.hasUnscopedBookingOrSupportIntent(session)) {
            return "de".equals(language)
                    ? "Soll das als neue Website, als KI-Chatbot auf einer Website oder als internes Automatisierungstool umgesetzt werden?"
                    : "Should this be implemented as a new website, an AI chatbot on a website, or an internal automation tool?";
        }
        if (projectContext.hasEcommerceIntent(session)) {
            if (!projectContext.hasPaymentInfo(session)) {
                return "de".equals(language)
                        ? "Welche Zahlungsarten möchten Sie im Online-Shop anbieten?"
                        : "Which payment methods should the online shop support?";
            }
            if (!projectContext.hasProductScaleInfo(session)) {
                return "de".equals(language)
                        ? "Wie viele Produkte oder Produktvarianten möchten Sie ungefähr anbieten?"
                        : "Roughly how many products or product variants do you plan to offer?";
            }
            return "de".equals(language)
                    ? "Bis wann möchten Sie mit dem Online-Shop ungefähr starten?"
                    : "When would you roughly like to start with the online shop?";
        }
        if (projectContext.hasWebsiteChatbotIntent(session) && !projectContext.hasChatbotPurpose(session)) {
            return "de".equals(language)
                    ? "Welche Aufgaben soll der KI-Chatbot auf der Website übernehmen?"
                    : "What tasks should the AI chatbot handle on the website?";
        }
        if (projectContext.hasWebsiteChatbotIntent(session)) {
            if (!projectContext.hasWebsiteOrSystemStatusInfo(session)) {
                return "de".equals(language)
                        ? "Gibt es bereits eine bestehende Website oder ein aktuelles System?"
                        : "Is there already an existing website or current system?";
            }
            if (!contactExtractor.conversationHasContactInfo(session.messages())) {
                return buildEmailContactQuestion(language);
            }
        }
        if (projectContext.containsAny(text, "rechnung", "rechnungen", "invoice", "invoicing")) {
            return "de".equals(language)
                    ? "Welche Software oder welchen aktuellen Ablauf nutzen Sie heute für die Rechnungen?"
                    : "What software or current process do you use for invoicing today?";
        }
        if (projectContext.containsAny(text, "website", "webseite", "redesign")) {
            if (projectContext.containsAny(text, "chatbot", "chat bot", "ki chatbot", "ai chatbot")) {
                return !contactExtractor.conversationHasContactInfo(session.messages())
                        ? buildEmailContactQuestion(language)
                        : languageSafety.buildLanguageSafeFallback(session, language);
            }
            if (projectContext.hasAnsweredNoToExistingWebsiteQuestion(session)) {
                return "de".equals(language)
                        ? "Welche Inhalte oder Funktionen soll die neue Website haben?"
                        : "What content or features should the new website include?";
            }
            return "de".equals(language)
                    ? "Gibt es bereits eine bestehende Website, die erneuert werden soll?"
                    : "Is there already an existing website that should be redesigned?";
        }
        if (projectContext.containsAny(text, "automation", "automatisierung", "automate", "automatisieren")) {
            return "de".equals(language)
                    ? "Welcher Ablauf soll als Erstes automatisiert werden?"
                    : "Which process should be automated first?";
        }
        return languageSafety.buildLanguageSafeFallback(session, language);
    }

    public String buildHumanContactReply(ChatbotCompanyConfig config, String language) {
        String email = config.fallbackContact().path("email").asText("");
        String contactUrl = config.handoff().path("url").asText("");
        if ("de".equals(language)) {
            return "Ich kann keinen Termin direkt vereinbaren und niemanden automatisch kontaktieren.\n\n"
                    + "Sie erreichen Bueno Web Solutions per E-Mail unter " + email + ".\n\n"
                    + "Für eine Projektanfrage können Sie auch das Kontaktformular verwenden:\n"
                    + contactUrl;
        }
        return "I cannot schedule an appointment directly or automatically contact the team.\n\n"
                + "You can reach Bueno Web Solutions by email at " + email + ".\n\n"
                + "For a project request, you can also use the contact form:\n"
                + contactUrl;
    }

    public String buildConcreteContactQuestion(String lastUserMessage, String language) {
        return buildEmailContactQuestion(lastUserMessage, language);
    }

    public String buildEmailContactQuestion(String language) {
        return buildEmailContactQuestion("", language);
    }

    private String buildEmailContactQuestion(String lastUserMessage, String language) {
        String text = lastUserMessage == null ? "" : lastUserMessage.toLowerCase(Locale.ROOT);
        boolean requestedOtherChannel = projectContext.containsAny(text, "telefon", "phone", "call", "anruf", "sms", "whatsapp");
        if ("de".equals(language)) {
            return requestedOtherChannel
                    ? "Für Projektanfragen nutzen wir E-Mail als Kontaktweg. Wie lautet Ihre E-Mail-Adresse?"
                    : "Wie lautet Ihre E-Mail-Adresse?";
        }
        return requestedOtherChannel
                ? "For project requests, we use email as the contact channel. What is your email address?"
                : "What is your email address?";
    }

    public String buildChannelClarificationQuestion(String lastUserMessage, String language) {
        String text = lastUserMessage == null ? "" : lastUserMessage.toLowerCase(Locale.ROOT);
        if ("de".equals(language)) {
            if (projectContext.containsAny(text, "sms")) return "Meinen Sie eine SMS-Automatisierung oder SMS nur als Kontaktweg?";
            if (projectContext.containsAny(text, "telefon", "phone", "call", "anruf")) return "Meinen Sie einen Telefonassistenten oder Telefon nur als Kontaktweg?";
            if (projectContext.containsAny(text, "whatsapp")) return "Meinen Sie eine WhatsApp-Automatisierung oder WhatsApp nur als Kontaktweg?";
            if (projectContext.containsAny(text, "mail", "email", "e-mail")) return "Meinen Sie eine E-Mail-Automatisierung oder E-Mail nur als Kontaktweg?";
            return "Meinen Sie damit den gewünschten Ablauf oder nur den Kontaktweg?";
        }
        if (projectContext.containsAny(text, "sms", "text message")) return "Do you mean SMS automation or SMS only as the contact channel?";
        if (projectContext.containsAny(text, "telefon", "phone", "call")) return "Do you mean a phone assistant or phone only as the contact channel?";
        if (projectContext.containsAny(text, "whatsapp")) return "Do you mean WhatsApp automation or WhatsApp only as the contact channel?";
        if (projectContext.containsAny(text, "mail", "email", "e-mail")) return "Do you mean email automation or email only as the contact channel?";
        return "Do you mean that as the process to automate or only as the contact channel?";
    }

    public boolean asksWrongChannelClarificationForWorkflow(String reply, ChatSession session) {
        String value = reply == null ? "" : reply;
        String lastUser = projectContext.getLastUserMessage(session).toLowerCase(Locale.ROOT);
        return CHANNEL_CLARIFICATION_PATTERN.matcher(value).find()
                && !projectContext.isContactPreferenceWithoutConcreteInfo(lastUser)
                && projectContext.containsAny(lastUser,
                        "rechnung", "rechnungen", "invoice", "invoicing", "mahnung", "zahlungserinnerung",
                        "automatisierung", "automation", "automatisch", "versenden", "senden", "schicken",
                        "erstellen", "generieren", "kunden", "customer", "antworten", "beantworten", "anfragen");
    }

    public String enforceFinalReplySafety(String reply, String language) {
        String cleaned = ChatbotText.cleanReply(reply)
                .replaceAll("(?i)\\bwe guarantee\\b", "we can discuss")
                .replaceAll("(?i)\\bguaranteed\\b", "planned")
                .replaceAll("(?i)\\bguaranteed results\\b", "planned results")
                .replaceAll("(?i)\\bguaranteed leads\\b", "possible leads")
                .replaceAll("(?i)\\bwill increase leads\\b", "can support lead generation")
                .replaceAll("(?i)\\bwill generate customers\\b", "can support customer inquiries")
                .replaceAll("(?i)\\bdefinitely improve\\b", "can improve")
                .replaceAll("(?i)\\bwir garantieren\\b", "wir können prüfen")
                .replaceAll("(?i)\\bgarantiert mehr\\b", "möglicherweise mehr")
                .replaceAll("(?i)\\bdefinitiv mehr\\b", "möglicherweise mehr")
                .replaceAll("(?i)\\bsicher mehr Kunden\\b", "kann bei der Kundengewinnung helfen")
                .replaceAll("(?i)\\bsicher mehr Anfragen\\b", "kann bei Anfragen helfen")
                .trim();
        return "de".equals(language) ? ChatbotText.normalizeGermanOutput(cleaned) : cleaned;
    }

    private boolean metadataRequestsContactTooEarly(AssistantMetadata metadata, ChatSession session) {
        if (projectContext.hasEnoughProjectContextForHandoff(session)) return false;
        String nextAction = metadata.nextAction() == null ? "" : metadata.nextAction().trim();
        String missingField = metadata.missingField() == null ? "" : metadata.missingField().trim();
        return metadata.contactRequired()
                || "ask_email".equalsIgnoreCase(nextAction)
                || "email".equalsIgnoreCase(missingField);
    }

    private boolean needsEarlyProjectFallback(String reply, ChatSession session) {
        String value = reply == null ? "" : reply;
        return !contactExtractor.conversationHasContactInfo(session.messages())
                && !replyLooksLikeTemplate(value)
                && EARLY_HANDOFF_PATTERN.matcher(value).find();
    }

    private boolean needsWeakProjectQuestionFallback(String reply, ChatSession session) {
        String value = reply == null ? "" : reply;
        return !contactExtractor.conversationHasContactInfo(session.messages())
                && !replyLooksLikeTemplate(value)
                && projectContext.getUserMessageCount(session) <= 1
                && (BAD_FILLER_PATTERN.matcher(value).find() || THANK_YOU_INQUIRY_PATTERN.matcher(value).find() || WEAK_QUESTION_PATTERN.matcher(value).find());
    }

    private boolean asksOptionalDetailAfterContact(ChatSession session, String reply) {
        return contactExtractor.conversationHasContactInfo(session.messages()) && projectContext.getUserMessageCount(session) >= 2 && reply != null && reply.contains("?") && !asksForContactOption(reply);
    }

    private boolean lastMessageHasContactAndLanguageSwitch(ChatSession session) {
        String last = projectContext.getLastUserMessage(session);
        return contactExtractor.textHasContactInfo(last) && languageSafety.isLanguageSwitchRequest(last);
    }

    private boolean asksWrongCurrentToolQuestionForWebsiteChatbot(String reply, ChatSession session) {
        String text = projectContext.userConversationText(session).toLowerCase(Locale.ROOT);
        return projectContext.getUserMessageCount(session) <= 1
                && projectContext.containsAny(text, "website", "webseite")
                && projectContext.containsAny(text, "chatbot", "chat bot", "ki chatbot", "ai chatbot")
                && CURRENT_PROCESS_TOOL_ASK_PATTERN.matcher(reply == null ? "" : reply).find();
    }

    private boolean asksAwkwardChatbotTaskQuestion(String reply) {
        return AWKWARD_CHATBOT_TASK_ASK_PATTERN.matcher(reply == null ? "" : reply).find();
    }

    private boolean asksKnownChatbotPurpose(String reply, ChatSession session) {
        return projectContext.hasWebsiteChatbotIntent(session)
                && projectContext.hasChatbotPurpose(session)
                && asksChatbotPurposeQuestion(reply);
    }

    private boolean asksKnownWebsiteOrSystemStatus(String reply, ChatSession session) {
        if (!projectContext.hasWebsiteOrSystemStatusInfo(session)) return false;
        String value = reply == null ? "" : reply;
        String normalized = value.toLowerCase(Locale.ROOT);
        return value.contains("?")
                && projectContext.containsAny(normalized,
                "bestehende website", "bestehende webseite", "aktuelles system", "bestehendes system",
                "existing website", "current system", "existing system");
    }

    private boolean asksChatbotPurposeQuestion(String reply) {
        String value = reply == null ? "" : reply;
        if (!value.contains("?")) return false;
        String normalized = value.toLowerCase(Locale.ROOT);
        return projectContext.containsAny(normalized, "chatbot", "chat bot", "ki-chatbot", "ki chatbot", "ai chatbot")
                && projectContext.containsAny(normalized,
                "aufgaben", "helfen", "übernehmen", "uebernehmen", "unterstützen", "unterstuetzen",
                "was soll", "what tasks", "what should", "handle", "support", "help", "do");
    }

    private boolean asksSpecificScopeForUnscopedFunctions(String reply, ChatSession session) {
        if (!projectContext.hasUnscopedBookingOrSupportIntent(session)) return false;
        String value = reply == null ? "" : reply;
        String normalized = value.toLowerCase(Locale.ROOT);
        if (EXISTING_WEBSITE_QUESTION_PATTERN.matcher(value).find()) return true;
        return projectContext.containsAny(normalized, "welche aufgaben soll der ki-chatbot", "what tasks should the ai chatbot", "ki-chatbot", "ai chatbot", "chatbot")
                && !projectContext.containsAny(normalized, "neue website", "internes automatisierungstool", "new website", "internal automation tool");
    }

    private boolean containsFalseForwardingClaim(String reply) {
        return FALSE_FORWARDING_CLAIM_PATTERN.matcher(reply == null ? "" : reply).find();
    }
}
