package com.buenws.buenws_backend.API.Chatbot;

public class ChatbotUnavailableException extends RuntimeException {
    private final int status;

    public ChatbotUnavailableException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
