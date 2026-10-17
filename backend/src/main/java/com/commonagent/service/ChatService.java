package com.commonagent.service;

import com.commonagent.agent.AgentWorkflow;
import com.commonagent.agent.state.AgentState;
import com.commonagent.agent.state.AgentState.SentimentResult;
import com.commonagent.dto.ChatRequest;
import com.commonagent.dto.ChatResponse;
import com.commonagent.model.Conversation;
import com.commonagent.model.Message;
import com.commonagent.model.User;
import com.commonagent.repository.ConversationRepository;
import com.commonagent.repository.MessageRepository;
import com.commonagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 聊天服务 - 核心业务编排
 * 负责会话管理、消息持久化、调用 Agent 工作流
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final AgentWorkflow agentWorkflow;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final UserRepository userRepo;

    /**
     * 处理用户消息
     */
    @Transactional
    public ChatResponse processMessage(ChatRequest request) {
        String tenantId = request.getTenantId();
        String userId = request.getUserId();

        // 1. 确保用户存在
        ensureUser(tenantId, userId, request.getUserName());

        // 2. 获取或创建会话
        Conversation conversation = getOrCreateConversation(tenantId, userId, request.getConversationId());

        // 3. 加载历史消息
        List<Message> history = messageRepo.findByConversationIdOrderByCreatedAtAsc(
                conversation.getConversationId());

        // 4. 保存用户消息
        Message userMessage = Message.builder()
                .tenantId(tenantId)
                .conversationId(conversation.getConversationId())
                .role("USER")
                .content(request.getMessage())
                .messageType(request.getMessageType())
                .createdAt(LocalDateTime.now())
                .build();
        messageRepo.save(userMessage);

        // 5. 构建 Agent 状态
        AgentState state = AgentState.builder()
                .tenantId(tenantId)
                .conversationId(conversation.getConversationId())
                .userId(userId)
                .userInput(request.getMessage())
                .history(history)
                .turnCount(conversation.getTurnCount() + 1)
                .build();

        // 6. 执行 Agent 工作流
        AgentState result = agentWorkflow.execute(state);

        // 7. 保存 Agent 回复
        Message agentMessage = Message.builder()
                .tenantId(tenantId)
                .conversationId(conversation.getConversationId())
                .role(result.isNeedHumanHandoff() ? "SYSTEM" : "ASSISTANT")
                .content(result.getGeneratedResponse())
                .messageType("TEXT")
                .intent(result.getIntentResult() != null ? result.getIntentResult().getIntentType() : null)
                .sentiment(result.getSentimentResult() != null ? result.getSentimentResult().getSentiment() : null)
                .referenceIds(buildReferenceIds(result))
                .createdAt(LocalDateTime.now())
                .build();
        messageRepo.save(agentMessage);

        // 8. 更新会话状态
        conversation.setTurnCount(conversation.getTurnCount() + 1);
        conversation.setCurrentIntent(
                result.getIntentResult() != null ? result.getIntentResult().getIntentType() : null);
        conversation.setSentiment(
                result.getSentimentResult() != null ? result.getSentimentResult().getSentiment() : "NEUTRAL");
        if (result.isNeedHumanHandoff()) {
            conversation.setStatus("TRANSFERRED");
            conversation.setTransferReason(result.getHandoffReason());
        }
        // 自动生成会话标题 (首次对话)
        if (conversation.getTitle() == null && conversation.getTurnCount() == 1) {
            conversation.setTitle(generateTitle(request.getMessage()));
        }
        conversationRepo.save(conversation);

        // 9. 构建响应
        return ChatResponse.builder()
                .conversationId(conversation.getConversationId())
                .messageId(UUID.randomUUID().toString())
                .content(result.getGeneratedResponse())
                .role(result.isNeedHumanHandoff() ? "SYSTEM" : "ASSISTANT")
                .intent(result.getIntentResult() != null ? result.getIntentResult().getIntentType() : null)
                .sentiment(result.getSentimentResult() != null ? result.getSentimentResult().getSentiment() : null)
                .needHumanHandoff(result.isNeedHumanHandoff())
                .handoffReason(result.getHandoffReason())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 获取会话历史
     */
    public List<Message> getConversationHistory(String tenantId, String conversationId) {
        return messageRepo.findByTenantIdAndConversationIdOrderByCreatedAtAsc(tenantId, conversationId);
    }

    /**
     * 获取用户会话列表
     */
    public List<Conversation> getUserConversations(String tenantId, String userId) {
        return conversationRepo.findByTenantIdAndUserIdOrderByUpdatedAtDesc(tenantId, userId);
    }

    private void ensureUser(String tenantId, String userId, String userName) {
        if (!userRepo.existsByTenantIdAndUserId(tenantId, userId)) {
            User user = User.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .name(userName != null ? userName : "用户" + userId.substring(0, 8))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepo.save(user);
        }
    }

    private Conversation getOrCreateConversation(String tenantId, String userId, String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationRepo.findByConversationId(conversationId)
                    .orElseGet(() -> createConversation(tenantId, userId));
        }
        return createConversation(tenantId, userId);
    }

    private Conversation createConversation(String tenantId, String userId) {
        Conversation conversation = Conversation.builder()
                .tenantId(tenantId)
                .conversationId(UUID.randomUUID().toString())
                .userId(userId)
                .status("ACTIVE")
                .turnCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return conversationRepo.save(conversation);
    }

    private String buildReferenceIds(AgentState state) {
        if (state.getKnowledgeResults() == null || state.getKnowledgeResults().isEmpty()) {
            return "[]";
        }
        List<String> ids = state.getKnowledgeResults().stream()
                .map(AgentState.KnowledgeResult::getKnowledgeId)
                .toList();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(ids);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String generateTitle(String firstMessage) {
        if (firstMessage == null) return "新会话";
        return firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
    }
}
