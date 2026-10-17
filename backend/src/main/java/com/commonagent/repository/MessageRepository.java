package com.commonagent.repository;

import com.commonagent.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    List<Message> findByConversationIdAndTenantIdOrderByCreatedAtAsc(String conversationId, String tenantId);
    List<Message> findByTenantIdAndConversationIdOrderByCreatedAtAsc(String tenantId, String conversationId);
}
