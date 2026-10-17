package com.commonagent.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 知识库条目表 (MySQL 存储元数据，ES 存储向量和全文)
 */
@Entity
@Table(name = "ca_knowledge")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    /** 知识条目唯一 ID (对应 ES 中的 document id) */
    @Column(nullable = false, unique = true, length = 64)
    private String knowledgeId;

    /** 标题 */
    @Column(nullable = false, length = 512)
    private String title;

    /** 内容摘要 (前200字) */
    @Column(length = 512)
    private String summary;

    /** 分类 */
    @Column(length = 64)
    private String category;

    /** 标签 (JSON Array) */
    @Column(length = 512)
    private String tags;

    /** 状态: ACTIVE, INACTIVE, DRAFT */
    @Column(length = 16)
    @Builder.Default
    private String status = "ACTIVE";

    /** 来源类型: MANUAL, IMPORT, CRAWL, API */
    @Column(length = 16)
    @Builder.Default
    private String sourceType = "MANUAL";

    /** 被引用次数 */
    @Builder.Default
    private Integer referenceCount = 0;

    /** 查看次数 */
    @Builder.Default
    private Integer viewCount = 0;

    /** 有效期限 (过期后自动下线) */
    private LocalDateTime expireAt;

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
