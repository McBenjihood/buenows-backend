package com.buenws.buenws_backend.API.Chatbot.Model;

import com.fasterxml.jackson.databind.JsonNode;

public record ChatbotCompanyConfig(
        String botName,
        String companyKey,
        String companyName,
        String subtitle,
        String welcomeMessage,
        String placeholder,
        String privacyNotice,
        JsonNode theme,
        JsonNode fallbackContact,
        JsonNode handoff,
        JsonNode locales,
        JsonNode businessInfo,
        JsonNode rules,
        JsonNode leadQuestions,
        String pricing
) {}
