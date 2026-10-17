package com.commonagent.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 会话表
 */
@Entity
@Table(name = "ca_conversation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    /** 会话唯一标识 (UUID) */
    @Column(nullable = false, unique = true, length = 64)
    private String conversationId;

    /** 关联用户 ID */
    @Column(nullable = false, length = 128)
    private String userId;

    /** 会话状态: ACTIVE, CLOSED, TRANSFERRED (转人工) */
    @Column(length = 16)
    @Builder.Default
    private String status = "ACTIVE";

    /** 当前意图类型 */
    @Column(length = 32)
    private String currentIntent;

    /** 用户情绪: POSITIVE, NEUTRAL, NEGATIVE */
    @Column(length = 16)
    @Builder.Default
    private String sentiment = "NEUTRAL";

    /** 消息轮次计数 */
    @Builder.Default
    private Integer turnCount = 0;

    /** 会话标题 (自动生成) */
    @Column(length = 256)
    private String title;

    /** 转人工原因 */
    @Column(length = 256)
    private String transferReason;

    /** 分配给的人工坐席 ID */
    @Column(length = 64)
    private String assignedAgentId;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
