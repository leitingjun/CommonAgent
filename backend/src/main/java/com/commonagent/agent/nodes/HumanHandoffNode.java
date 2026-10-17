package com.commonagent.agent.nodes;

import com.commonagent.agent.WorkflowNode;
import com.commonagent.agent.state.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 转人工判断节点
 * 根据多种条件判断是否需要转接人工坐席:
 * 1. 负面情绪超过阈值
 * 2. 用户明确要求转人工
 * 3. 连续多轮无法解决问题
 * 4. 意图置信度过低
 * 5. 特定意图类型 (如投诉升级)
 */
@Slf4j
@Component
public class HumanHandoffNode implements WorkflowNode {

    @Value("${agent.sentiment-negative-threshold:0.6}")
    private double negativeThreshold;

    @Value("${agent.intent-confidence-threshold:0.75}")
    private double confidenceThreshold;

    @Value("${agent.max-conversation-turns:10}")
    private int maxTurns;

    /** 连续无法解决的最大轮次 */
    private static final int MAX_UNRESOLVED_TURNS = 3;

    @Override
    public AgentState execute(AgentState state) {
        log.debug("[HumanHandoff] tenant={}, turn={}", state.getTenantId(), state.getTurnCount());

        boolean needHandoff = false;
        String reason = null;

        // 1. 检查用户是否明确要求转人工
        if (isExplicitHandoffRequest(state.getUserInput())) {
            needHandoff = true;
            reason = "用户明确要求转人工";
        }

        // 2. 检查负面情绪
        if (!needHandoff && state.getSentimentResult() != null) {
            if ("NEGATIVE".equals(state.getSentimentResult().getSentiment())
                    && state.getSentimentResult().getScore() >= negativeThreshold) {
                // 检查情绪标签是否为愤怒/沮丧
                String emotion = state.getSentimentResult().getEmotionLabel();
                if ("ANGRY".equals(emotion) || "FRUSTRATED".equals(emotion)) {
                    needHandoff = true;
                    reason = "检测到强烈负面情绪: " + emotion;
                }
            }
        }

        // 3. 检查意图置信度过低
        if (!needHandoff && state.getIntentResult() != null) {
            if (state.getIntentResult().getConfidence() < confidenceThreshold) {
                needHandoff = true;
                reason = "意图置信度过低: " + state.getIntentResult().getConfidence();
            }
        }

        // 4. 超过最大对话轮次
        if (!needHandoff && state.getTurnCount() >= maxTurns) {
            needHandoff = true;
            reason = "对话轮次超限: " + state.getTurnCount();
        }

        // 5. 投诉类意图 + 负面情绪
        if (!needHandoff && state.getIntentResult() != null
                && "COMPLAINT".equals(state.getIntentResult().getIntentType())) {
            if (state.getSentimentResult() != null
                    && "NEGATIVE".equals(state.getSentimentResult().getSentiment())) {
                needHandoff = true;
                reason = "投诉+负面情绪，建议转人工处理";
            }
        }

        state.setNeedHumanHandoff(needHandoff);
        state.setHandoffReason(reason);
        state.setCurrentNode(getNodeName());

        if (needHandoff) {
            log.info("[HumanHandoff] Transferring to human - reason: {}", reason);
        }

        return state;
    }

    private boolean isExplicitHandoffRequest(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase().trim();
        return lower.contains("转人工") || lower.contains("人工客服")
                || lower.contains("人工服务") || lower.contains("找人工")
                || lower.contains("real person") || lower.contains("human agent")
                || lower.contains("speak to someone");
    }

    @Override
    public String getNodeName() {
        return "HUMAN_HANDOFF";
    }
}
