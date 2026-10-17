package com.commonagent.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.commonagent.config.ElasticsearchConfig;
import com.commonagent.dto.KnowledgeDTO;
import com.commonagent.model.KnowledgeDocument;
import com.commonagent.repository.KnowledgeBaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 知识库服务 - ES 不可用时自动降级，仅用 MySQL 存储
 */
@Slf4j
@Service
@Lazy
public class KnowledgeService {

    private final ElasticsearchClient esClient;
    private final ElasticsearchConfig esConfig;
    private final KnowledgeBaseRepository knowledgeRepo;
    private final LLMService llmService;

    public KnowledgeService(
            @Autowired(required = false) ElasticsearchClient esClient,
            ElasticsearchConfig esConfig,
            KnowledgeBaseRepository knowledgeRepo,
            LLMService llmService) {
        this.esClient = esClient;
        this.esConfig = esConfig;
        this.knowledgeRepo = knowledgeRepo;
        this.llmService = llmService;
    }

    private boolean esAvailable() {
        return esClient != null;
    }

    public void ensureTenantIndex(String tenantId) {
        if (!esAvailable()) {
            log.debug("[KnowledgeService] ES not available, skip index creation");
            return;
        }
        String indexName = getIndexName(tenantId);
        try {
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (!exists) {
                createIndex(indexName);
                log.info("[KnowledgeService] Created ES index: {}", indexName);
            }
        } catch (Exception e) {
            log.error("[KnowledgeService] Failed to ensure index: {}", indexName, e);
        }
    }

    private void createIndex(String indexName) throws Exception {
        esClient.indices().create(CreateIndexRequest.of(c -> c
                .index(indexName)
                .settings(IndexSettings.of(s -> s
                        .numberOfShards("1")
                        .numberOfReplicas("0")
                ))
                .mappings(TypeMapping.of(m -> m
                        .properties("title", Property.of(p -> p
                                .text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart"))))
                        .properties("content", Property.of(p -> p
                                .text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart"))))
                        .properties("category", Property.of(p -> p.keyword(k -> k)))
                        .properties("tags", Property.of(p -> p.keyword(k -> k)))
                        .properties("content_vector", Property.of(p -> p
                                .denseVector(d -> d.dims(1536).similarity("cosine"))))
                        .properties("tenant_id", Property.of(p -> p.keyword(k -> k)))
                        .properties("created_at", Property.of(p -> p.date(d -> d)))
                ))
        ));
    }

    /**
     * 添加知识文档（同时写入 MySQL 和 ES）
     */
    @Transactional
    public KnowledgeDocument addKnowledge(String tenantId, KnowledgeDTO dto) {
        ensureTenantIndex(tenantId);

        String knowledgeId = UUID.randomUUID().toString();
        float[] vector = llmService.embed(dto.getContent());

        // 写入 MySQL
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .tenantId(tenantId)
                .knowledgeId(knowledgeId)
                .title(dto.getTitle())
                .summary(truncate(dto.getContent(), 200))
                .category(dto.getCategory())
                .tags(dto.getTags())
                .status("ACTIVE")
                .sourceType(dto.getSourceType() != null ? dto.getSourceType() : "MANUAL")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        knowledgeRepo.save(doc);

        // 写入 ES
        if (esAvailable()) {
            try {
                Map<String, Object> esDoc = new HashMap<>();
                esDoc.put("title", dto.getTitle());
                esDoc.put("content", dto.getContent());
                esDoc.put("category", dto.getCategory());
                esDoc.put("tags", dto.getTags());
                esDoc.put("content_vector", vector);
                esDoc.put("tenant_id", tenantId);
                esDoc.put("created_at", LocalDateTime.now().toString());

                esClient.index(i -> i
                        .index(getIndexName(tenantId))
                        .id(knowledgeId)
                        .document(esDoc)
                );
            } catch (Exception e) {
                log.error("[KnowledgeService] Failed to index to ES", e);
            }
        }

        log.info("[KnowledgeService] Added knowledge: {} - {}", knowledgeId, dto.getTitle());
        return doc;
    }

    /**
     * 删除知识文档
     */
    @Transactional
    public void deleteKnowledge(String tenantId, String knowledgeId) {
        knowledgeRepo.deleteByKnowledgeId(knowledgeId);
        if (esAvailable()) {
            try {
                esClient.delete(d -> d.index(getIndexName(tenantId)).id(knowledgeId));
            } catch (Exception e) {
                log.warn("[KnowledgeService] Failed to delete from ES: {}", knowledgeId, e);
            }
        }
    }

    /**
     * 批量导入知识
     */
    @Transactional
    public int batchImport(String tenantId, List<KnowledgeDTO> items) {
        int count = 0;
        for (KnowledgeDTO item : items) {
            try {
                addKnowledge(tenantId, item);
                count++;
            } catch (Exception e) {
                log.error("[KnowledgeService] Batch import error for: {}", item.getTitle(), e);
            }
        }
        return count;
    }

    public List<KnowledgeDocument> listKnowledge(String tenantId, String status) {
        return knowledgeRepo.findByTenantIdAndStatusOrderByUpdatedAtDesc(
                tenantId, status != null ? status : "ACTIVE");
    }

    private String getIndexName(String tenantId) {
        return esConfig.getIndexPrefix() + "_" + tenantId;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
