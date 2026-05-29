package com.buenws.buenws_backend.API.Chatbot.Guard;

import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.ChatSession;
import com.buenws.buenws_backend.API.Chatbot.Util.ProjectContextEvaluator;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class LanguageSafetyGuard {
    private static final Pattern LANGUAGE_SWITCH_PATTERN = Pattern.compile("\\b(antworte|antworten|reply|respond|answer)\\b.{0,40}\\b(deutsch|englisch|german|english)\\b", Pattern.CASE_INSENSITIVE);

    private final ProjectContextEvaluator projectContext;

    public LanguageSafetyGuard(ProjectContextEvaluator projectContext) {
        this.projectContext = projectContext;
    }

    public boolean replyLooksWrongLanguage(String reply, String language) {
        if (reply == null || reply.isBlank()) return false;
        return "de".equals(language)
                ? Pattern.compile("\\b(Please|What current|Could you|Desired solution|The session language|Thank you, this is a clear request)\\b", Pattern.CASE_INSENSITIVE).matcher(reply).find()
                : Pattern.compile("\\b(Bitte|Welche|Koennen|Können|Gewuenschte Loesung|Gewünschte Lösung|Die Sitzungssprache|Danke, das ist eine klare Anfrage)\\b", Pattern.CASE_INSENSITIVE).matcher(reply).find();
    }

    public boolean isLanguageLockOnlyReply(String reply) {
        return reply != null && Pattern.compile("\\b(session language stays fixed|Sitzungssprache bleibt)\\b", Pattern.CASE_INSENSITIVE).matcher(reply).find() && reply.trim().length() < 120;
    }

    public boolean missesLanguageLockAcknowledgement(String reply, ChatSession session, String language) {
        if (!isLanguageSwitchRequest(projectContext.getLastUserMessage(session))) return false;
        return "de".equals(language)
                ? !Pattern.compile("\\bSitzungssprache bleibt Deutsch\\b", Pattern.CASE_INSENSITIVE).matcher(reply == null ? "" : reply).find()
                : !Pattern.compile("\\bsession language stays fixed in English\\b", Pattern.CASE_INSENSITIVE).matcher(reply == null ? "" : reply).find();
    }

    public boolean isLanguageSwitchRequest(String value) {
        return LANGUAGE_SWITCH_PATTERN.matcher(value == null ? "" : value).find();
    }

    public String languageLockSentence(String language) {
        return "de".equals(language) ? "Die Sitzungssprache bleibt Deutsch." : "The session language stays fixed in English.";
    }

    public String buildLanguageSafeFallback(ChatSession session, String language) {
        String question = projectContext.getLastAssistantQuestion(session);
        return !question.isBlank() ? question : ("de".equals(language) ? "Welche Information ist für die Anfrage als Nächstes am wichtigsten?" : "What information is most important for the request next?");
    }
}
