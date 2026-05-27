package com.buenws.buenws_backend.API.Chatbot;

public class ChatbotRateLimitExceededException extends RuntimeException {
    private final String code;
    private final String error;
    private final int retryAfterSeconds;

    public ChatbotRateLimitExceededException(String code, String error, int retryAfterSeconds) {
        super(error);
        this.code = code;
        this.error = error;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String code() { return code; }
    public String error() { return error; }
    public int retryAfterSeconds() { return retryAfterSeconds; }
}
