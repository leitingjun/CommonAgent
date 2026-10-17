package com.commonagent.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 用户表 (可与租户关联的终端用户)
 */
@Entity
@Table(name = "ca_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, unique = true, length = 128)
    private String userId;

    @Column(length = 64)
    private String name;

    @Column(length = 128)
    private String email;

    @Column(length = 20)
    private String phone;

    /** 用户标签 JSON (VIP, 普通用户等) */
    @Column(columnDefinition = "TEXT")
    private String tags;

    /** 用户自定义字段 JSON */
    @Column(columnDefinition = "TEXT")
    private String extraFields;

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
