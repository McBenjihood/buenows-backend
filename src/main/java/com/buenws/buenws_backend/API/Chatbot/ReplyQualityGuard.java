package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.AssistantMetadata;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatSession;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ReplyQualityGuard {
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\b(Gewuenschte Loesung|Gewünschte Lösung|Desired solution|Nachricht|Message)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTACT_ASK_PATTERN = Pattern.compile("\\b(best contact|best way to contact|contact you|email address|phone number|Kontaktmoeglichkeit|Kontaktmöglichkeit|kontaktieren|E-Mail-Adresse|Telefonnummer|per Mail melden)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BAD_FILLER_PATTERN = Pattern.compile("\\b(Das klingt|Es klingt|Danke fuer die Informationen|Danke für die Informationen|Es waere hilfreich|Es wäre hilfreich|Ich benoetige noch|Ich benötige noch|Moechten Sie|Möchten Sie|Koennten Sie|Könnten Sie|That sounds|It sounds|To clarify|This could involve|It would be helpful|Would you like to|Could you please|we can develop|we could develop)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE_PROMISE_PATTERN = Pattern.compile("\\b(we guarantee|we promise|guaranteed results|guaranteed leads|guaranteed customers|wir garantieren|wir versprechen|garantierte kunden|garantierte anfragen|sicher mehr kunden|sicher mehr anfragen)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern THANK_YOU_INQUIRY_PATTERN = Pattern.compile("\\b(Vielen Dank fuer Ihre Anfrage|Vielen Dank für Ihre Anfrage|Vielen Dank f\\u00C3\\u00BCr Ihre Anfrage|Thank you for your inquiry)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEAK_QUESTION_PATTERN = Pattern.compile("\\b(Could you|K.nnten)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[[^\\]]+]\\(https?://[^)]+\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EARLY_HANDOFF_PATTERN = Pattern.compile("\\b(contact form|contact page|project request|Projekt anfragen|Kontaktformular|Kontaktseite|Formular|Anfrageformular|Handoff)\\b|https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTACT_PREFERENCE_PATTERN = Pattern.compile("\\b(sms|text message|telefon|phone|call|anruf|whatsapp|e-mail|email|mail)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CURRENT_PROCESS_TOOL_ASK_PATTERN = Pattern.compile("\\b(aktuelle?r? Ablauf|bestehende?s Tool|current process|current tool|software|tools?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AWKWARD_CHATBOT_TASK_ASK_PATTERN = Pattern.compile("\\bWas soll der KI-Chatbot\\b.{0,140}\\bhelfen\\b", Pattern.CASE_INSENSITIVE);

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
        if (asksForContactOption(reply) && !projectContext.hasEnoughProjectContextForHandoff(session)) return "The reply asks for contact information before the project need is clear. Ask what process, task or current workflow should be improved first.";
        if (projectContext.repeatsPreviousAssistantQuestion(reply, session)) return "The reply repeats the previous assistant question after the user answered it. Ask the next useful project question instead.";
        if (asksWrongCurrentToolQuestionForWebsiteChatbot(reply, session)) return "The user wants a website with an AI chatbot. Ask what the chatbot should do on the website instead of asking for a current process or tool.";
        if (asksAwkwardChatbotTaskQuestion(reply)) return "The German question is grammatically awkward. Ask: Welche Aufgaben soll der KI-Chatbot auf der Website übernehmen?";
        if (metadata.readyForHandoff() && !replyLooksLikeTemplate(reply)) return "The metadata says handoff is ready but the reply does not use the exact contact-form template.";
        if (contactExtractor.conversationHasContactInfo(session.messages()) && asksForContactOption(reply)) return "The user already provided contact information. Generate the contact-form template if the project is concrete enough.";
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
                || asksAwkwardChatbotTaskQuestion(reply);
    }

    public boolean needsContactDelayFallback(String reply, ChatSession session) {
        return asksForContactOption(reply) && !projectContext.hasEnoughProjectContextForHandoff(session);
    }

    public boolean lastUserContactButProjectContextMissing(ChatSession session) {
        return contactExtractor.textHasContactInfo(projectContext.getLastUserMessage(session))
                && !projectContext.hasEnoughProjectContextForHandoff(session);
    }

    public boolean replyLooksLikeTemplate(String reply) {
        return TEMPLATE_PATTERN.matcher(reply == null ? "" : reply).find();
    }

    public boolean asksForContactOption(String reply) {
        return CONTACT_ASK_PATTERN.matcher(reply == null ? "" : reply).find() && (reply == null || reply.contains("?"));
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
        String text = projectContext.conversationText(session).toLowerCase(Locale.ROOT);
        if (projectContext.containsAny(text, "rechnung", "rechnungen", "invoice", "invoicing")) {
            return "de".equals(language)
                    ? "Welche Software oder welchen aktuellen Ablauf nutzen Sie heute für die Rechnungen?"
                    : "What software or current process do you use for invoicing today?";
        }
        if (projectContext.containsAny(text, "website", "webseite", "redesign")) {
            if (projectContext.containsAny(text, "chatbot", "chat bot", "ki chatbot", "ai chatbot")) {
                return "de".equals(language)
                        ? "Welche Aufgaben soll der KI-Chatbot auf der Website übernehmen?"
                        : "What tasks should the AI chatbot handle on the website?";
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

    public String buildConcreteContactQuestion(String lastUserMessage, String language) {
        String text = lastUserMessage == null ? "" : lastUserMessage.toLowerCase(Locale.ROOT);
        if ("de".equals(language)) {
            if (projectContext.containsAny(text, "telefon", "phone", "call", "anruf", "sms", "whatsapp")) return "Wie lautet die Telefonnummer, unter der wir Sie erreichen können?";
            if (projectContext.containsAny(text, "mail", "email", "e-mail")) return "Wie lautet Ihre E-Mail-Adresse?";
            return "Wie lautet Ihre E-Mail-Adresse oder Telefonnummer?";
        }
        if (projectContext.containsAny(text, "telefon", "phone", "call", "anruf", "sms", "whatsapp")) return "What phone number can we use to contact you?";
        if (projectContext.containsAny(text, "mail", "email", "e-mail")) return "What is your email address?";
        return "What email address or phone number can we use to contact you?";
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

    public String enforceFinalReplySafety(String reply, String language) {
        String cleaned = ChatbotText.cleanReply(reply)
                .replaceAll("(?i)\\bwe guarantee\\b", "we can discuss")
                .replaceAll("(?i)\\bguaranteed\\b", "planned")
                .replaceAll("(?i)\\bwir garantieren\\b", "wir können prüfen")
                .trim();
        return "de".equals(language) ? ChatbotText.normalizeGermanOutput(cleaned) : cleaned;
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
        String text = projectContext.conversationText(session).toLowerCase(Locale.ROOT);
        return projectContext.getUserMessageCount(session) <= 1
                && projectContext.containsAny(text, "website", "webseite")
                && projectContext.containsAny(text, "chatbot", "chat bot", "ki chatbot", "ai chatbot")
                && CURRENT_PROCESS_TOOL_ASK_PATTERN.matcher(reply == null ? "" : reply).find();
    }

    private boolean asksAwkwardChatbotTaskQuestion(String reply) {
        return AWKWARD_CHATBOT_TASK_ASK_PATTERN.matcher(reply == null ? "" : reply).find();
    }
}
