package com.commonagent.config;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class EmbeddingConfig {

    @Value("${llm.default.api-key:}")
    private String apiKey;

    @Value("${llm.default.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Bean
    @ConditionalOnExpression("!'${llm.default.api-key:}'.isEmpty()")
    public OpenAiEmbeddingModel openAiEmbeddingModel() {
        log.info("[EmbeddingConfig] Creating OpenAiEmbeddingModel with API key");
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName("text-embedding-3-small")
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(OpenAiEmbeddingModel.class)
    public OpenAiEmbeddingModel stubEmbeddingModel() {
        log.warn("[EmbeddingConfig] No API Key, using stub EmbeddingModel");
        return OpenAiEmbeddingModel.builder()
                .apiKey("sk-stub")
                .modelName("text-embedding-3-small")
                .timeout(Duration.ofSeconds(5))
                .build();
    }
}
