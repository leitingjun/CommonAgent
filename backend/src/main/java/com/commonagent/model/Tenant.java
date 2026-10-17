package com.commonagent.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 租户表 - 支持多租户隔离
 */
@Entity
@Table(name = "ca_tenant")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 租户唯一标识 */
    @Column(nullable = false, unique = true, length = 64)
    private String tenantId;

    /** 租户名称 */
    @Column(nullable = false, length = 128)
    private String name;

    /** 所属行业: general, ecommerce, finance, healthcare, education 等 */
    @Column(length = 32)
    @Builder.Default
    private String industry = "general";

    /** 租户状态: ACTIVE, INACTIVE, SUSPENDED */
    @Column(length = 16)
    @Builder.Default
    private String status = "ACTIVE";

    /** Agent 配置 JSON */
    @Column(columnDefinition = "TEXT")
    private String agentConfig;

    /** LLM 配置 JSON (覆盖默认 LLM 配置) */
    @Column(columnDefinition = "TEXT")
    private String llmConfig;

    /** 自定义 Prompt 模板 */
    @Column(columnDefinition = "TEXT")
    private String promptTemplate;

    /** 欢迎语 */
    @Column(length = 512)
    private String welcomeMessage;

    /** 最大并发会话数 */
    @Builder.Default
    private Integer maxConcurrentSessions = 100;

    /** 联系邮箱 */
    @Column(length = 128)
    private String contactEmail;

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
