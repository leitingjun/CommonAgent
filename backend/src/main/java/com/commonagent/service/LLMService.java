package com.commonagent.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LLM 服务 - 封装大模型调用 (文本生成、向量化等)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMService {

    private final OpenAiEmbeddingModel embeddingModel;

    /**
     * 文本向量化
     */
    public float[] embed(String text) {
        try {
            Embedding embedding = embeddingModel.embed(text).content();
            float[] vector = embedding.vectorAsList().stream()
                    .map(Float::valueOf)
                    .collect(
                            () -> new float[embedding.vectorAsList().size()],
                            (arr, val) -> arr[arr.length - embedding.vectorAsList().size() + 
                                    embedding.vectorAsList().indexOf(val)] = val,
                            (a, b) -> {}
                    );
            // 简化实现
            List<Float> floatList = embedding.vectorAsList();
            float[] result = new float[floatList.size()];
            for (int i = 0; i < floatList.size(); i++) {
                result[i] = floatList.get(i);
            }
            return result;
        } catch (Exception e) {
            log.error("[LLMService] Embedding failed", e);
            return new float[1536]; // 返回零向量
        }
    }
}
