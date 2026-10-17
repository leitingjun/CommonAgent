package com.commonagent.agent.nodes;

import com.commonagent.agent.WorkflowNode;
import com.commonagent.agent.state.AgentState;
import com.commonagent.agent.state.AgentState.IntentResult;
import com.commonagent.config.LLMConfig;
import com.commonagent.model.Tenant;
import com.commonagent.repository.TenantRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 意图分类节点
 * 使用 LLM 识别用户意图，支持按行业自定义意图体系
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentClassificationNode implements WorkflowNode {

    private final OpenAiChatModel chatModel;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @Override
    public AgentState execute(AgentState state) {
        log.debug("[IntentClassification] tenant={}, conversation={}, input={}",
                state.getTenantId(), state.getConversationId(), state.getUserInput());

        try {
            Tenant tenant = tenantRepository.findByTenantId(state.getTenantId()).orElse(null);

            String intentSchema = buildIntentSchema(tenant);
            String prompt = buildPrompt(state.getUserInput(), intentSchema, state.getConversationSummary());

            String response = chatModel.generate(UserMessage.from(prompt)).content().text();
            IntentResult result = parseIntentResponse(response);

            state.setIntentResult(result);
            state.setCurrentNode(getNodeName());
            log.info("[IntentClassification] intent={}, confidence={}", result.getIntentType(), result.getConfidence());

        } catch (Exception e) {
            log.error("[IntentClassification] error", e);
            state.setIntentResult(IntentResult.builder()
                    .intentType("OTHER")
                    .confidence(0.5)
                    .build());
        }

        return state;
    }

    private String buildIntentSchema(Tenant tenant) {
        // 根据租户行业返回不同意图体系
        String industry = tenant != null ? tenant.getIndustry() : "general";

        return switch (industry.toLowerCase()) {
            case "ecommerce" -> """
                - ORDER_QUERY: 查询订单状态、物流信息
                - REFUND: 退款/退货申请
                - PRODUCT_INQUIRY: 商品咨询
                - COMPLAINT: 投诉建议
                - ACCOUNT: 账户相关问题
                - GREETING: 问候/寒暄
                - OTHER: 其他
                """;
            case "finance" -> """
                - ACCOUNT_QUERY: 账户查询
                - TRANSACTION: 交易相关问题
                - LOAN: 贷款咨询
                - CARD_SERVICE: 银行卡服务
                - COMPLAINT: 投诉
                - GREETING: 问候
                - OTHER: 其他
                """;
            case "healthcare" -> """
                - APPOINTMENT: 预约挂号
                - PRESCRIPTION: 处方咨询
                - INSURANCE: 医保咨询
                - MEDICAL_RECORD: 病历查询
                - EMERGENCY: 紧急情况
                - GREETING: 问候
                - OTHER: 其他
                """;
            default -> """
                - INQUIRY: 一般咨询
                - COMPLAINT: 投诉建议
                - TECHNICAL_SUPPORT: 技术支持
                - ORDER_QUERY: 订单/业务查询
                - REFUND: 退款/售后
                - GREETING: 问候/寒暄
                - OTHER: 其他
                """;
        };
    }

    private String buildPrompt(String userInput, String intentSchema, String history) {
        return """
            你是一个客服意图分类器。请分析用户消息，确定其意图类型。

            ## 意图分类体系:
            %s

            ## 对话历史:
            %s

            ## 用户最新消息:
            %s

            ## 要求:
            请以 JSON 格式返回分析结果:
            {
                "intentType": "意图类型",
                "subIntent": "子意图(可选)",
                "confidence": 0.95,
                "entities": {"key": "value"}
            }

            只返回 JSON，不要其他内容。
            """.formatted(intentSchema, history.isEmpty() ? "无" : history, userInput);
    }

    private IntentResult parseIntentResponse(String response) {
        try {
            // 提取 JSON
            String json = response.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            return IntentResult.builder()
                    .intentType((String) map.getOrDefault("intentType", "OTHER"))
                    .subIntent((String) map.get("subIntent"))
                    .confidence(map.get("confidence") instanceof Number n ? n.doubleValue() : 0.5)
                    .entities(getEntities(map))
                    .build();
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse intent response: {}", response);
            return IntentResult.builder().intentType("OTHER").confidence(0.5).build();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getEntities(Map<String, Object> map) {
        Object entitiesObj = map.get("entities");
        if (entitiesObj instanceof Map) {
            return (Map<String, String>) entitiesObj;
        }
        return Map.of();
    }

    @Override
    public String getNodeName() {
        return "INTENT_CLASSIFICATION";
    }
}
