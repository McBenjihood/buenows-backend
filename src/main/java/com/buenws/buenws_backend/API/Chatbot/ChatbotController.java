package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatRequest;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ChatResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ConfigResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.ErrorResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.HealthResponse;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.SessionRequest;
import com.buenws.buenws_backend.API.Chatbot.ChatbotModels.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {
    private final ChatbotService chatbotService;
    private final ChatbotRateLimitService rateLimitService;

    public ChatbotController(ChatbotService chatbotService, ChatbotRateLimitService rateLimitService) {
        this.chatbotService = chatbotService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(chatbotService.health());
    }

    @GetMapping("/config")
    public ResponseEntity<ConfigResponse> config(@RequestParam(value = "language", required = false) String language,
                                                 @RequestHeader(value = "X-Chat-Language", required = false) String headerLanguage) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(chatbotService.publicConfig(firstNonBlank(language, headerLanguage)));
    }

    @PostMapping("/session")
    public ResponseEntity<SessionResponse> session(@RequestBody(required = false) SessionRequest sessionRequest, HttpServletRequest request) {
        String language = sessionRequest == null ? null : sessionRequest.language();
        rateLimitService.checkSessionLimits(ChatbotRequestUtil.getClientKey(request), ChatbotText.resolveLanguage(language));
        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.CACHE_CONTROL, "no-store").body(chatbotService.createSession(language));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        String language = ChatbotText.resolveLanguage(chatRequest == null ? null : chatRequest.language());
        rateLimitService.checkChatLimits(ChatbotRequestUtil.getClientKey(request), language);
        ChatResponse response = chatbotService.chat(chatRequest == null ? null : chatRequest.sessionId(), language, chatRequest == null ? null : chatRequest.message());
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(response);
    }

    @ExceptionHandler(ChatbotRateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(ChatbotRateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.retryAfterSeconds()))
                .body(new ErrorResponse(exception.error(), exception.code(), exception.retryAfterSeconds()));
    }

    @ExceptionHandler(ChatbotValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ChatbotValidationException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(ChatbotUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(ChatbotUnavailableException exception) {
        HttpStatus status = exception.status() == 503 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(OpenAiResponsesClient.OpenAiClientException.class)
    public ResponseEntity<ErrorResponse> handleOpenAi(OpenAiResponsesClient.OpenAiClientException exception) {
        HttpStatus status = exception.status() == 401 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = exception.status() == 401 ? ChatbotText.t(ChatbotText.DEFAULT_LANGUAGE, "openAiInvalid") : ChatbotText.t(ChatbotText.DEFAULT_LANGUAGE, "chatUnavailable");
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(ChatbotText.t(ChatbotText.DEFAULT_LANGUAGE, "chatUnavailable")));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return ChatbotText.DEFAULT_LANGUAGE;
    }
}
