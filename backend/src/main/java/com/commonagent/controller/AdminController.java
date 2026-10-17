package com.commonagent.controller;

import com.commonagent.agent.AgentWorkflow;
import com.commonagent.repository.ConversationRepository;
import com.commonagent.repository.KnowledgeBaseRepository;
import com.commonagent.repository.MessageRepository;
import com.commonagent.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台控制器 - 提供系统级统计数据
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TenantRepository tenantRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final KnowledgeBaseRepository knowledgeRepository;
    private final AgentWorkflow agentWorkflow;

    /**
     * 系统概览统计
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        long tenantCount = tenantRepository.count();
        long activeConversations = conversationRepository.countByTenantIdAndStatus("__all__", "ACTIVE");
        long totalKnowledge = knowledgeRepository.count();

        return ResponseEntity.ok(Map.of(
                "tenantCount", tenantCount,
                "activeConversations", activeConversations,
                "totalKnowledge", totalKnowledge,
                "workflowGraph", agentWorkflow.getGraphDescription()
        ));
    }
}
