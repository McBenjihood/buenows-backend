package com.buenws.buenws_backend.API.Chatbot.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatbotConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
