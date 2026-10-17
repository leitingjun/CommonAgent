package com.commonagent.repository;

import com.commonagent.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeDocument, Long> {
    Optional<KnowledgeDocument> findByKnowledgeId(String knowledgeId);
    List<KnowledgeDocument> findByTenantIdAndStatusOrderByUpdatedAtDesc(String tenantId, String status);
    List<KnowledgeDocument> findByTenantIdAndCategory(String tenantId, String category);
    long countByTenantIdAndStatus(String tenantId, String status);
    void deleteByKnowledgeId(String knowledgeId);
}
