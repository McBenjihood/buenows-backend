package com.buenws.buenws_backend.API.Chatbot.Controller;

import com.buenws.buenws_backend.API.Chatbot.Client.OpenAiResponsesClient;
import com.buenws.buenws_backend.API.Chatbot.Configuration.ChatbotProperties;
import com.buenws.buenws_backend.API.Chatbot.Exception.ChatbotRateLimitExceededException;
import com.buenws.buenws_backend.API.Chatbot.Exception.ChatbotUnavailableException;
import com.buenws.buenws_backend.API.Chatbot.Exception.ChatbotValidationException;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.ChatRequest;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.ChatResponse;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.ConfigResponse;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.ErrorResponse;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.HealthResponse;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.SessionRequest;
import com.buenws.buenws_backend.API.Chatbot.Model.ChatbotModels.SessionResponse;
import com.buenws.buenws_backend.API.Chatbot.Service.ChatbotRateLimitService;
import com.buenws.buenws_backend.API.Chatbot.Service.ChatbotService;
import com.buenws.buenws_backend.API.Chatbot.Util.ChatbotRequestUtil;
import com.buenws.buenws_backend.API.Chatbot.Util.ChatbotText;
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
    private final ChatbotProperties properties;

    public ChatbotController(ChatbotService chatbotService, ChatbotRateLimitService rateLimitService, ChatbotProperties properties) {
        this.chatbotService = chatbotService;
        this.rateLimitService = rateLimitService;
        this.properties = properties;
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
        rateLimitService.checkSessionLimits(ChatbotRequestUtil.getClientKey(request, properties), ChatbotText.resolveLanguage(language));
        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.CACHE_CONTROL, "no-store").body(chatbotService.createSession(language));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        String language = ChatbotText.resolveLanguage(chatRequest == null ? null : chatRequest.language());
        String sessionId = chatRequest == null ? null : chatRequest.sessionId();
        String responseLanguage = chatbotService.resolveLanguageForRequest(sessionId, language);
        rateLimitService.checkChatLimits(ChatbotRequestUtil.getClientKey(request, properties), responseLanguage);
        try {
            ChatResponse response = chatbotService.chat(sessionId, language, chatRequest == null ? null : chatRequest.message());
            return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(response);
        } catch (OpenAiResponsesClient.OpenAiClientException exception) {
            HttpStatus status = exception.status() == 401 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.INTERNAL_SERVER_ERROR;
            throw new ChatbotUnavailableException(ChatbotText.t(responseLanguage, "chatUnavailable"), status.value());
        }
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
        return ResponseEntity.status(status).body(new ErrorResponse(ChatbotText.t(ChatbotText.DEFAULT_LANGUAGE, "chatUnavailable")));
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
