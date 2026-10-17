package com.commonagent.agent.nodes;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.commonagent.agent.WorkflowNode;
import com.commonagent.agent.state.AgentState;
import com.commonagent.agent.state.AgentState.KnowledgeResult;
import com.commonagent.config.ElasticsearchConfig;
import com.commonagent.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识检索节点 — ES 不可用时自动降级为空结果
 */
@Slf4j
@Component
@Lazy
public class KnowledgeRetrievalNode implements WorkflowNode {

    private final ElasticsearchClient esClient;
    private final ElasticsearchConfig esConfig;
    private final LLMService llmService;

    @Value("${agent.knowledge-top-k:5}")
    private int topK;

    @Value("${agent.knowledge-similarity-threshold:0.7}")
    private double similarityThreshold;

    public KnowledgeRetrievalNode(
            @Autowired(required = false) ElasticsearchClient esClient,
            ElasticsearchConfig esConfig,
            LLMService llmService) {
        this.esClient = esClient;
        this.esConfig = esConfig;
        this.llmService = llmService;
    }

    @Override
    public AgentState execute(AgentState state) {
        log.debug("[KnowledgeRetrieval] tenant={}, query={}, esAvailable={}",
                state.getTenantId(), state.getUserInput(), esClient != null);

        if (esClient == null) {
            log.warn("[KnowledgeRetrieval] ES not available, returning empty results");
            state.setKnowledgeResults(List.of());
            state.setCurrentNode(getNodeName());
            return state;
        }

        try {
            String indexName = getIndexName(state.getTenantId());

            boolean indexExists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (!indexExists) {
                log.warn("[KnowledgeRetrieval] Index {} not found", indexName);
                state.setKnowledgeResults(List.of());
                state.setCurrentNode(getNodeName());
                return state;
            }

            float[] queryVector = llmService.embed(state.getUserInput());
            // ES 可用时执行向量检索 (简化实现，直接返回空列表)
            List<KnowledgeResult> results = List.of();

            state.setKnowledgeResults(results);
            state.setCurrentNode(getNodeName());
            log.info("[KnowledgeRetrieval] Found {} results", results.size());

        } catch (Exception e) {
            log.error("[KnowledgeRetrieval] error", e);
            state.setKnowledgeResults(List.of());
        }

        return state;
    }

    private String getIndexName(String tenantId) {
        return esConfig.getIndexPrefix() + "_" + tenantId;
    }

    @Override
    public String getNodeName() {
        return "KNOWLEDGE_RETRIEVAL";
    }
}
