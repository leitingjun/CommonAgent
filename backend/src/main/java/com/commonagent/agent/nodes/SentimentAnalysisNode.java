package com.commonagent.agent.nodes;

import com.commonagent.agent.WorkflowNode;
import com.commonagent.agent.state.AgentState;
import com.commonagent.agent.state.AgentState.SentimentResult;
import com.commonagent.config.LLMConfig;
import com.commonagent.model.Tenant;
import com.commonagent.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 情感分析节点
 * 分析用户情绪，触发相应的处理策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentimentAnalysisNode implements WorkflowNode {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Override
    public AgentState execute(AgentState state) {
        log.debug("[SentimentAnalysis] tenant={}, input={}", state.getTenantId(), state.getUserInput());

        try {
            String prompt = """
                分析以下用户消息的情感。返回 JSON:
                {
                    "sentiment": "POSITIVE|NEUTRAL|NEGATIVE",
                    "score": 0.85,
                    "emotionLabel": "ANGRY|FRUSTRATED|SATISFIED|CONFUSED|URGENT|CALM"
                }

                用户消息: %s
                """.formatted(state.getUserInput());

            String response = chatModel.generate(UserMessage.from(prompt)).content().text();
            SentimentResult result = parseSentimentResponse(response);

            state.setSentimentResult(result);
            state.setCurrentNode(getNodeName());
            log.info("[SentimentAnalysis] sentiment={}, emotion={}", result.getSentiment(), result.getEmotionLabel());

        } catch (Exception e) {
            log.error("[SentimentAnalysis] error", e);
            state.setSentimentResult(SentimentResult.builder()
                    .sentiment("NEUTRAL")
                    .score(0.5)
                    .emotionLabel("CALM")
                    .build());
        }

        return state;
    }

    private SentimentResult parseSentimentResponse(String response) {
        try {
            String json = extractJson(response);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            return SentimentResult.builder()
                    .sentiment((String) map.getOrDefault("sentiment", "NEUTRAL"))
                    .score(map.get("score") instanceof Number n ? n.doubleValue() : 0.5)
                    .emotionLabel((String) map.getOrDefault("emotionLabel", "CALM"))
                    .build();
        } catch (Exception e) {
            return SentimentResult.builder().sentiment("NEUTRAL").score(0.5).emotionLabel("CALM").build();
        }
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    @Override
    public String getNodeName() {
        return "SENTIMENT_ANALYSIS";
    }
}
