package com.buenws.buenws_backend.API.Chatbot;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SensitiveDataGuard {
    private static final Pattern PASSWORD_OR_SECRET_PATTERN = Pattern.compile(
            "\\b(passwort|password|pwd|secret|api[ -]?key|token|login)\\b.{0,50}\\b(ist|is|=|:)\\s*\\S{6,}",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern IBAN_PATTERN = Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z0-9]{11,30}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CARD_CANDIDATE_PATTERN = Pattern.compile("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)");
    private static final Pattern ID_DOCUMENT_PATTERN = Pattern.compile(
            "\\b(ausweis|identit[aä]tskarte|id document|passport|reisepass|passnummer)\\b.{0,40}\\d{4,}",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public boolean containsSensitiveData(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) return false;
        if (PASSWORD_OR_SECRET_PATTERN.matcher(text).find()) return true;
        if (IBAN_PATTERN.matcher(text.replaceAll("\\s+", "")).find()) return true;
        if (ID_DOCUMENT_PATTERN.matcher(text).find()) return true;
        return containsPaymentCardNumber(text);
    }

    private boolean containsPaymentCardNumber(String text) {
        Matcher matcher = CARD_CANDIDATE_PATTERN.matcher(text);
        while (matcher.find()) {
            String digits = matcher.group().replaceAll("\\D", "");
            if (digits.length() >= 13 && digits.length() <= 19 && looksLikeCardContext(text, matcher.start()) && passesLuhn(digits)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeCardContext(String text, int startIndex) {
        String context = text.substring(Math.max(0, startIndex - 80), Math.min(text.length(), startIndex + 80)).toLowerCase(Locale.ROOT);
        return context.contains("karte")
                || context.contains("kreditkarte")
                || context.contains("credit card")
                || context.contains("card number")
                || context.contains("cvv")
                || context.contains("cvc")
                || context.contains("zahlung");
    }

    private boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int index = digits.length() - 1; index >= 0; index -= 1) {
            int digit = digits.charAt(index) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
