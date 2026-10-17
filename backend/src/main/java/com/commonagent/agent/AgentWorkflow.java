package com.commonagent.agent;

import com.commonagent.agent.nodes.*;
import com.commonagent.agent.state.AgentState;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Agent 工作流引擎 - LangGraph 风格的图状态机
 *
 * 工作流图:
 *   START
 *     │
 *     ▼
 *   INTENT_CLASSIFICATION
 *     │
 *     ├──(需要动作执行)──▶ ACTION_EXECUTION ──▶ KNOWLEDGE_RETRIEVAL
 *     │                                           │
 *     └──(不需要动作)──▶ KNOWLEDGE_RETRIEVAL ◀────┘
 *                            │
 *                            ▼
 *                      SENTIMENT_ANALYSIS
 *                            │
 *                            ▼
 *                      HUMAN_HANDOFF
 *                       │         │
 *                 (不需要转人工)  (需要转人工)
 *                       │         │
 *                       ▼         ▼
 *              RESPONSE_GENERATION  TRANSFER_TO_HUMAN
 *                       │
 *                       ▼
 *                      END
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentWorkflow {



    private final IntentClassificationNode intentClassificationNode;
    private final KnowledgeRetrievalNode knowledgeRetrievalNode;
    private final SentimentAnalysisNode sentimentAnalysisNode;
    private final HumanHandoffNode humanHandoffNode;
    private final ResponseGenerationNode responseGenerationNode;
    private final ActionExecutionNode actionExecutionNode;

    /** 图定义: 节点名 -> 节点执行器 */
    private final Map<String, WorkflowNode> nodes = new LinkedHashMap<>();

    /** 条件边: 节点名 -> 路由函数 (返回下一节点名) */
    private final Map<String, Function<AgentState, String>> conditionalEdges = new LinkedHashMap<>();

    public AgentWorkflow(IntentClassificationNode intentClassificationNode, KnowledgeRetrievalNode knowledgeRetrievalNode, SentimentAnalysisNode sentimentAnalysisNode, HumanHandoffNode humanHandoffNode, ResponseGenerationNode responseGenerationNode, ActionExecutionNode actionExecutionNode) {
        this.intentClassificationNode = intentClassificationNode;
        this.knowledgeRetrievalNode = knowledgeRetrievalNode;
        this.sentimentAnalysisNode = sentimentAnalysisNode;
        this.humanHandoffNode = humanHandoffNode;
        this.responseGenerationNode = responseGenerationNode;
        this.actionExecutionNode = actionExecutionNode;
    }

    @PostConstruct
    public void init() {
        // 注册节点
        nodes.put("INTENT_CLASSIFICATION", intentClassificationNode);
        nodes.put("ACTION_EXECUTION", actionExecutionNode);
        nodes.put("KNOWLEDGE_RETRIEVAL", knowledgeRetrievalNode);
        nodes.put("SENTIMENT_ANALYSIS", sentimentAnalysisNode);
        nodes.put("HUMAN_HANDOFF", humanHandoffNode);
        nodes.put("RESPONSE_GENERATION", responseGenerationNode);

        // 定义条件路由
        conditionalEdges.put("INTENT_CLASSIFICATION", this::routeAfterIntent);
        conditionalEdges.put("ACTION_EXECUTION", state -> "KNOWLEDGE_RETRIEVAL");
        conditionalEdges.put("KNOWLEDGE_RETRIEVAL", state -> "SENTIMENT_ANALYSIS");
        conditionalEdges.put("SENTIMENT_ANALYSIS", state -> "HUMAN_HANDOFF");
        conditionalEdges.put("HUMAN_HANDOFF", this::routeAfterHandoff);
    }

    /**
     * 执行完整工作流
     * @param state 初始状态 (包含 tenantId, userId, userInput 等)
     * @return 最终状态 (包含 generatedResponse)
     */
    public AgentState execute(AgentState state) {
        log.info("[AgentWorkflow] Starting workflow - tenant={}, conversation={}, turn={}",
                state.getTenantId(), state.getConversationId(), state.getTurnCount());

        String currentNode = "INTENT_CLASSIFICATION";
        int maxSteps = 20; // 防止死循环
        int step = 0;

        while (currentNode != null && !"END".equals(currentNode) && step < maxSteps) {
            step++;
            log.debug("[AgentWorkflow] Step {}: executing node {}", step, currentNode);

            // 执行当前节点
            WorkflowNode node = nodes.get(currentNode);
            if (node == null) {
                log.error("[AgentWorkflow] Unknown node: {}", currentNode);
                break;
            }

            state = node.execute(state);

            // 获取下一节点
            Function<AgentState, String> router = conditionalEdges.get(currentNode);
            if (router != null) {
                currentNode = router.apply(state);
            } else {
                currentNode = "END";
            }

            log.debug("[AgentWorkflow] Step {}: next node = {}", step, currentNode);
        }

        state.setCompleted(true);
        log.info("[AgentWorkflow] Workflow completed in {} steps", step);
        return state;
    }

    /**
     * 意图分类后的路由
     */
    private String routeAfterIntent(AgentState state) {
        if (state.getIntentResult() == null) {
            return "KNOWLEDGE_RETRIEVAL";
        }

        // 需要执行业务动作的意图类型
        String intentType = state.getIntentResult().getIntentType();
        boolean needsAction = "ORDER_QUERY".equals(intentType)
                || "REFUND".equals(intentType)
                || "APPOINTMENT".equals(intentType)
                || "ACCOUNT_QUERY".equals(intentType)
                || "TRANSACTION".equals(intentType);

        return needsAction ? "ACTION_EXECUTION" : "KNOWLEDGE_RETRIEVAL";
    }

    /**
     * 转人工判断后的路由
     */
    private String routeAfterHandoff(AgentState state) {
        if (state.isNeedHumanHandoff()) {
            // 转人工 - 生成转接提示
            state.setGeneratedResponse(buildTransferMessage(state));
            return "END";
        }
        return "RESPONSE_GENERATION";
    }

    private String buildTransferMessage(AgentState state) {
        String reason = state.getHandoffReason() != null ? state.getHandoffReason() : "需要更专业的服务";
        return """
            您的请求已收到，由于%s，我将为您转接人工客服。
            预计等待时间不超过2分钟，请稍候...

            【系统提示: 会话已转接至人工坐席队列】
            """.formatted(reason);
    }

    /**
     * 获取工作流可视化描述 (用于调试)
     */
    public String getGraphDescription() {
        return """
            ┌──────────────────────┐
            │  INTENT_CLASSIFICATION│
            └──────────┬───────────┘
                       │
                 ┌─────┴─────┐
            (需要动作)    (不需要动作)
                 │             │
                 ▼             │
            ┌─────────┐       │
            │ACTION    │       │
            │EXECUTION │       │
            └────┬─────┘       │
                 │             │
                 └──────┬──────┘
                        ▼
            ┌──────────────────┐
            │KNOWLEDGE_RETRIEVAL│
            └────────┬─────────┘
                     ▼
            ┌──────────────────┐
            │SENTIMENT_ANALYSIS │
            └────────┬─────────┘
                     ▼
            ┌──────────────────┐
            │  HUMAN_HANDOFF    │
            └───┬──────────┬────┘
           (转人工)     (不转人工)
                │            │
                ▼            ▼
        ┌──────────┐  ┌──────────────────┐
        │TRANSFER  │  │RESPONSE_GENERATION│
        │TO_HUMAN  │  └────────┬─────────┘
        └──────────┘           ▼
                            [END]
            """;
    }
}
