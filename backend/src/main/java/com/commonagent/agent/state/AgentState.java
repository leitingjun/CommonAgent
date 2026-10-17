package com.commonagent.agent.state;

import com.commonagent.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 状态 - LangGraph 工作流的状态对象
 * 在整个工作流中传递和更新
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentState {

    /** 租户 ID */
    private String tenantId;

    /** 会话 ID */
    private String conversationId;

    /** 用户 ID */
    private String userId;

    /** 当前用户输入 */
    private String userInput;

    /** 对话历史消息列表 */
    @Builder.Default
    private List<Message> history = new ArrayList<>();

    /** 当前轮次 */
    @Builder.Default
    private int turnCount = 0;

    // ===== 各节点输出 =====

    /** 意图分类结果 */
    private IntentResult intentResult;

    /** 知识检索结果 */
    private List<KnowledgeResult> knowledgeResults;

    /** 情感分析结果 */
    private SentimentResult sentimentResult;

    /** 生成的回复内容 */
    private String generatedResponse;

    /** 是否需要转人工 */
    @Builder.Default
    private boolean needHumanHandoff = false;

    /** 转人工原因 */
    private String handoffReason;

    /** 执行的动作结果 */
    private ActionResult actionResult;

    /** 当前工作流节点 */
    private String currentNode;

    /** 下一个工作流节点 */
    private String nextNode;

    /** 工作流是否完成 */
    @Builder.Default
    private boolean completed = false;

    /** 错误信息 */
    private String error;

    /** 扩展元数据 */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    // ===== 内部类 =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntentResult {
        /** 意图类型: INQUIRY, COMPLAINT, ORDER_QUERY, TECHNICAL_SUPPORT, REFUND, GREETING, OTHER */
        private String intentType;
        /** 子意图 */
        private String subIntent;
        /** 置信度 0-1 */
        private double confidence;
        /** 实体提取结果 */
        @Builder.Default
        private Map<String, String> entities = new HashMap<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeResult {
        private String knowledgeId;
        private String title;
        private String content;
        /** 相似度分数 */
        private double score;
        /** 分类 */
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentResult {
        /** POSITIVE, NEUTRAL, NEGATIVE */
        private String sentiment;
        private double score;
        /** 情绪标签: ANGRY, FRUSTRATED, SATISFIED, CONFUSED, URGENT */
        private String emotionLabel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionResult {
        private String actionType;
        private boolean success;
        private String resultData;
        private String errorMessage;
    }

    // ===== 辅助方法 =====

    public void addMessage(Message message) {
        if (this.history == null) {
            this.history = new ArrayList<>();
        }
        this.history.add(message);
    }

    public String getConversationSummary() {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, history.size() - 10); // 最近10轮
        for (int i = start; i < history.size(); i++) {
            Message msg = history.get(i);
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }
}
