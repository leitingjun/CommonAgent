package com.commonagent.agent.nodes;

import com.commonagent.agent.WorkflowNode;
import com.commonagent.agent.state.AgentState;
import com.commonagent.agent.state.AgentState.KnowledgeResult;
import com.commonagent.config.LLMConfig;
import com.commonagent.model.Tenant;
import com.commonagent.repository.TenantRepository;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 响应生成节点
 * 基于检索到的知识、意图分类结果和对话历史，生成自然语言回复
 * 支持按租户自定义 Prompt 模板
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseGenerationNode implements WorkflowNode {

    private final OpenAiChatModel chatModel;
    private final TenantRepository tenantRepository;

    @Override
    public AgentState execute(AgentState state) {
        log.debug("[ResponseGeneration] tenant={}, intent={}",
                state.getTenantId(),
                state.getIntentResult() != null ? state.getIntentResult().getIntentType() : "unknown");

        try {
            Tenant tenant = tenantRepository.findByTenantId(state.getTenantId()).orElse(null);

            String systemPrompt = buildSystemPrompt(tenant, state);
            String userPrompt = buildUserPrompt(state);

            String response = chatModel.generate(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(userPrompt)).content().text();

            state.setGeneratedResponse(response);
            state.setCurrentNode(getNodeName());
            log.info("[ResponseGeneration] generated {} chars", response.length());

        } catch (Exception e) {
            log.error("[ResponseGeneration] error", e);
            state.setGeneratedResponse(getFallbackResponse(state));
        }

        return state;
    }

    private String buildSystemPrompt(Tenant tenant, AgentState state) {
        // 租户自定义 Prompt 模板优先
        if (tenant != null && tenant.getPromptTemplate() != null && !tenant.getPromptTemplate().isBlank()) {
            return tenant.getPromptTemplate();
        }

        // 根据行业使用不同 Prompt
        String industry = tenant != null ? tenant.getIndustry() : "general";

        String basePrompt = switch (industry.toLowerCase()) {
            case "ecommerce" -> """
                你是 %s 的智能客服助手。你的职责是:
                1. 友好、专业地回答用户关于订单、商品、退款等问题
                2. 准确提供物流信息、退换货政策等
                3. 遇到无法解决的问题时，主动引导用户转人工
                4. 保持耐心，理解用户情绪

                回复要求:
                - 简洁明了，重点突出
                - 使用友好、温暖但不失专业的语气
                - 必要时使用列表或分步骤说明
                """;
            case "finance" -> """
                你是 %s 的金融客服助手。你的职责是:
                1. 准确回答关于账户、交易、贷款等金融业务问题
                2. 严格遵守金融合规要求
                3. 涉及敏感操作时必须引导用户通过官方渠道办理
                4. 保护用户隐私，绝不索要密码等敏感信息

                回复要求:
                - 专业严谨，数据准确
                - 涉及合规内容必须注明"具体以官方公告为准"
                - 风险提示必不可少
                """;
            default -> """
                你是 %s 的智能客服助手。你的职责是:
                1. 准确、高效地回答用户问题
                2. 根据知识库提供专业建议
                3. 遇到复杂问题主动引导转人工
                4. 保持友好耐心的服务态度

                回复要求:
                - 回答准确、简洁
                - 不确定的内容主动说明
                - 保持专业和友好
                """;
        };

        String companyName = tenant != null ? tenant.getName() : "我们公司";
        return String.format(basePrompt, companyName);
    }

    private String buildUserPrompt(AgentState state) {
        StringBuilder sb = new StringBuilder();

        // 检索到的知识内容
        if (state.getKnowledgeResults() != null && !state.getKnowledgeResults().isEmpty()) {
            sb.append("## 参考知识:\n");
            for (int i = 0; i < state.getKnowledgeResults().size(); i++) {
                KnowledgeResult kr = state.getKnowledgeResults().get(i);
                sb.append(String.format("%d. [%s] %s\n", i + 1, kr.getCategory(), kr.getContent()));
            }
            sb.append("\n");
        }

        // 对话历史
        String history = state.getConversationSummary();
        if (!history.isEmpty()) {
            sb.append("## 对话历史:\n").append(history).append("\n");
        }

        // 用户当前消息
        sb.append("## 用户消息:\n").append(state.getUserInput()).append("\n");
        sb.append("\n请根据上述参考知识和对话历史，生成合适的回复:");

        return sb.toString();
    }

    private String getFallbackResponse(AgentState state) {
        return "抱歉，我暂时无法处理您的问题。请稍后再试，或联系我们的客服人员获取帮助。";
    }

    @Override
    public String getNodeName() {
        return "RESPONSE_GENERATION";
    }
}
