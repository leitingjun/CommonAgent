package com.commonagent.controller;

import com.commonagent.dto.KnowledgeDTO;
import com.commonagent.model.KnowledgeDocument;
import com.commonagent.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /**
     * 添加知识条目
     */
    @PostMapping
    public ResponseEntity<KnowledgeDocument> addKnowledge(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody KnowledgeDTO dto) {
        KnowledgeDocument doc = knowledgeService.addKnowledge(tenantId, dto);
        return ResponseEntity.ok(doc);
    }

    /**
     * 获取知识库列表
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeDocument>> listKnowledge(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String status) {
        List<KnowledgeDocument> list = knowledgeService.listKnowledge(tenantId, status);
        return ResponseEntity.ok(list);
    }

    /**
     * 删除知识条目
     */
    @DeleteMapping("/{knowledgeId}")
    public ResponseEntity<Map<String, String>> deleteKnowledge(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String knowledgeId) {
        knowledgeService.deleteKnowledge(tenantId, knowledgeId);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    /**
     * 批量导入知识
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchImport(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody List<KnowledgeDTO> items) {
        int count = knowledgeService.batchImport(tenantId, items);
        return ResponseEntity.ok(Map.of("imported", count, "total", items.size()));
    }

    /**
     * 确保索引存在
     */
    @PostMapping("/index/ensure")
    public ResponseEntity<Map<String, String>> ensureIndex(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        knowledgeService.ensureTenantIndex(tenantId);
        return ResponseEntity.ok(Map.of("message", "索引已就绪"));
    }
}
