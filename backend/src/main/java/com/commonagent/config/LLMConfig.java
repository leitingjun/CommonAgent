package com.commonagent.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LLMConfig {

    private DefaultConfig defaultConfig = new DefaultConfig();
    private Map<String, TenantLLMConfig> tenants;

    @Data
    public static class DefaultConfig {
        private String provider = "openai";
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o";
        private double temperature = 0.7;
        private int maxTokens = 2048;
    }

    @Data
    public static class TenantLLMConfig {
        private String provider;
        private String apiKey;
        private String baseUrl;
        private String model;
        private Double temperature;
        private Integer maxTokens;
    }

    /**
     * 获取指定租户的 LLM 配置，如未配置则返回默认
     */
    public TenantLLMConfig getTenantConfig(String tenantId) {
        TenantLLMConfig config = tenants != null ? tenants.get(tenantId) : null;
        if (config == null) {
            config = new TenantLLMConfig();
            config.setProvider(defaultConfig.getProvider());
            config.setApiKey(defaultConfig.getApiKey());
            config.setBaseUrl(defaultConfig.getBaseUrl());
            config.setModel(defaultConfig.getModel());
            config.setTemperature(defaultConfig.getTemperature());
            config.setMaxTokens(defaultConfig.getMaxTokens());
        }
        return config;
    }

    @Bean
    @ConditionalOnExpression("!'${llm.default.api-key:}'.isEmpty()")
    public OpenAiChatModel openAiChatModel() {
        log.info("[LLMConfig] Creating OpenAiChatModel with API key");
        return OpenAiChatModel.builder()
                .apiKey(defaultConfig.getApiKey())
                .baseUrl(defaultConfig.getBaseUrl())
                .modelName(defaultConfig.getModel())
                .temperature(defaultConfig.getTemperature())
                .maxTokens(defaultConfig.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    @ConditionalOnExpression("!'${llm.default.api-key:}'.isEmpty()")
    public OpenAiStreamingChatModel openAiStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(defaultConfig.getApiKey())
                .baseUrl(defaultConfig.getBaseUrl())
                .modelName(defaultConfig.getModel())
                .temperature(defaultConfig.getTemperature())
                .maxTokens(defaultConfig.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * 存根 ChatModel — 无 API Key 时的兜底，让 Spring 容器能正常启动
     * 实际调用会失败，但不会阻塞启动
     */
    @Bean
    @ConditionalOnMissingBean(OpenAiChatModel.class)
    public OpenAiChatModel stubChatModel() {
        log.warn("[LLMConfig] No API Key configured, using stub ChatModel (calls will fail at runtime)");
        return OpenAiChatModel.builder()
                .apiKey("sk-stub")
                .modelName("gpt-3.5-turbo")
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(OpenAiStreamingChatModel.class)
    public OpenAiStreamingChatModel stubStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey("sk-stub")
                .modelName("gpt-3.5-turbo")
                .timeout(Duration.ofSeconds(5))
                .build();
    }
}
