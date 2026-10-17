package com.commonagent.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 消息表
 */
@Entity
@Table(name = "ca_message")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String conversationId;

    /** 角色: USER, ASSISTANT, SYSTEM, HUMAN_AGENT */
    @Column(nullable = false, length = 16)
    private String role;

    /** 消息内容 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 消息类型: TEXT, IMAGE, FILE, SYSTEM_EVENT */
    @Column(length = 16)
    @Builder.Default
    private String messageType = "TEXT";

    /** 意图识别结果 */
    @Column(length = 32)
    private String intent;

    /** 情感分析结果 */
    @Column(length = 16)
    private String sentiment;

    /** 引用的知识条目 ID 列表 (JSON Array) */
    @Column(columnDefinition = "TEXT")
    private String referenceIds;

    /** 元数据 JSON */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
