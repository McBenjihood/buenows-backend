package com.buenws.buenws_backend.API.Chatbot;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.RateLimitService;
import com.buenws.buenws_backend.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/chatbot")
public class ChatbotAdminController {
    private final ChatbotConversationHistoryService historyService;
    private final RateLimitService rateLimitService;

    public ChatbotAdminController(ChatbotConversationHistoryService historyService, RateLimitService rateLimitService) {
        this.historyService = historyService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/companies")
    public ResponseEntity<Records.ApiResponse<List<Records.AdminChatbotCompanyResponse>>> getCompanies(HttpServletRequest request) {
        rateLimitService.checkBucket("chatbot-companies:get:" + RequestUtil.getClientIp(request), 5);
        return ResponseEntity.ok(historyService.getCompanies());
    }

    @GetMapping("/conversations")
    public ResponseEntity<Records.ApiResponse<List<Records.AdminChatbotConversationSummaryResponse>>> getConversations(
            @RequestParam(value = "companyKey", required = false) String companyKey,
            HttpServletRequest request) {
        rateLimitService.checkBucket("chatbot-conversations:get:" + RequestUtil.getClientIp(request), 5);
        return ResponseEntity.ok(historyService.getConversationSummaries(companyKey));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<Records.ApiResponse<Records.AdminChatbotConversationDetailResponse>> getConversation(
            @PathVariable UUID conversationId,
            HttpServletRequest request) {
        rateLimitService.checkBucket("chatbot-conversation-detail:get:" + RequestUtil.getClientIp(request), 10);
        return ResponseEntity.ok(historyService.getConversationDetail(conversationId));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Records.ApiResponse<Void>> deleteConversation(
            @PathVariable UUID conversationId,
            HttpServletRequest request) {
        rateLimitService.checkBucket("chatbot-conversation:delete:" + RequestUtil.getClientIp(request), 15);
        return ResponseEntity.ok(historyService.deleteConversation(conversationId));
    }
}
