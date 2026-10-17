package com.commonagent.controller;

import com.commonagent.dto.ChatRequest;
import com.commonagent.dto.ChatResponse;
import com.commonagent.model.Conversation;
import com.commonagent.model.Message;
import com.commonagent.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 聊天控制器 - 核心对话接口
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送消息 (同步)
     */
    @PostMapping("/send")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.processMessage(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/history/{conversationId}")
    public ResponseEntity<List<Message>> getHistory(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String conversationId) {
        List<Message> history = chatService.getConversationHistory(tenantId, conversationId);
        return ResponseEntity.ok(history);
    }

    /**
     * 获取用户会话列表
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> getConversations(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String userId) {
        List<Conversation> conversations = chatService.getUserConversations(tenantId, userId);
        return ResponseEntity.ok(conversations);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "CommonAgent Chat"));
    }
}
