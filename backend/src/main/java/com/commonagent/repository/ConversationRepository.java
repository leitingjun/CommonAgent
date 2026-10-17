package com.commonagent.repository;

import com.commonagent.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByConversationId(String conversationId);
    List<Conversation> findByTenantIdAndUserIdOrderByUpdatedAtDesc(String tenantId, String userId);
    List<Conversation> findByTenantIdAndStatusOrderByUpdatedAtDesc(String tenantId, String status);
    long countByTenantIdAndStatus(String tenantId, String status);
}
