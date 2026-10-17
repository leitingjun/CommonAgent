-- ============================================
-- CommonAgent 数据库初始化脚本
-- 支持多租户的通用客服 Agent 系统
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS common_agent
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE common_agent;

-- ============================================
-- 1. 租户表
-- ============================================
CREATE TABLE IF NOT EXISTS ca_tenant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL UNIQUE COMMENT '租户唯一标识',
    name VARCHAR(128) NOT NULL COMMENT '租户名称',
    industry VARCHAR(32) DEFAULT 'general' COMMENT '所属行业: general, ecommerce, finance, healthcare, education',
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT '租户状态: ACTIVE, INACTIVE, SUSPENDED',
    agent_config TEXT COMMENT 'Agent 配置 JSON',
    llm_config TEXT COMMENT 'LLM 配置 JSON',
    prompt_template TEXT COMMENT '自定义 Prompt 模板',
    welcome_message VARCHAR(512) COMMENT '欢迎语',
    max_concurrent_sessions INT DEFAULT 100 COMMENT '最大并发会话数',
    contact_email VARCHAR(128) COMMENT '联系邮箱',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_status (status),
    INDEX idx_tenant_industry (industry)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

-- ============================================
-- 2. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS ca_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL COMMENT '租户ID',
    user_id VARCHAR(128) NOT NULL COMMENT '用户唯一标识',
    name VARCHAR(64) COMMENT '用户名',
    email VARCHAR(128) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '电话',
    tags TEXT COMMENT '用户标签 JSON',
    extra_fields TEXT COMMENT '自定义字段 JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_user (tenant_id, user_id),
    INDEX idx_user_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 3. 会话表
-- ============================================
CREATE TABLE IF NOT EXISTS ca_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL COMMENT '租户ID',
    conversation_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话唯一标识',
    user_id VARCHAR(128) NOT NULL COMMENT '用户ID',
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, CLOSED, TRANSFERRED',
    current_intent VARCHAR(32) COMMENT '当前意图类型',
    sentiment VARCHAR(16) DEFAULT 'NEUTRAL' COMMENT 'POSITIVE, NEUTRAL, NEGATIVE',
    turn_count INT DEFAULT 0 COMMENT '消息轮次',
    title VARCHAR(256) COMMENT '会话标题',
    transfer_reason VARCHAR(256) COMMENT '转人工原因',
    assigned_agent_id VARCHAR(64) COMMENT '人工坐席ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conv_tenant_user (tenant_id, user_id),
    INDEX idx_conv_tenant_status (tenant_id, status),
    INDEX idx_conv_status (status),
    INDEX idx_conv_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ============================================
-- 4. 消息表
-- ============================================
CREATE TABLE IF NOT EXISTS ca_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL COMMENT '租户ID',
    conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    role VARCHAR(16) NOT NULL COMMENT 'USER, ASSISTANT, SYSTEM, HUMAN_AGENT',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type VARCHAR(16) DEFAULT 'TEXT' COMMENT 'TEXT, IMAGE, FILE, SYSTEM_EVENT',
    intent VARCHAR(32) COMMENT '意图识别结果',
    sentiment VARCHAR(16) COMMENT '情感分析结果',
    reference_ids TEXT COMMENT '引用知识条目ID列表 JSON',
    metadata TEXT COMMENT '扩展元数据 JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_msg_conversation (conversation_id),
    INDEX idx_msg_tenant_conv (tenant_id, conversation_id),
    INDEX idx_msg_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ============================================
-- 5. 知识库表
-- ============================================
CREATE TABLE IF NOT EXISTS ca_knowledge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL COMMENT '租户ID',
    knowledge_id VARCHAR(64) NOT NULL UNIQUE COMMENT '知识条目唯一ID',
    title VARCHAR(512) NOT NULL COMMENT '标题',
    summary VARCHAR(512) COMMENT '内容摘要',
    category VARCHAR(64) COMMENT '分类',
    tags VARCHAR(512) COMMENT '标签 JSON',
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, DRAFT',
    source_type VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'MANUAL, IMPORT, CRAWL, API',
    reference_count INT DEFAULT 0 COMMENT '被引用次数',
    view_count INT DEFAULT 0 COMMENT '查看次数',
    expire_at DATETIME COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kb_tenant_status (tenant_id, status),
    INDEX idx_kb_tenant_category (tenant_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- ============================================
-- 初始数据: 插入默认租户
-- ============================================
INSERT INTO ca_tenant (tenant_id, name, industry, status, welcome_message, prompt_template)
VALUES ('demo', '演示公司', 'general', 'ACTIVE',
        '您好！我是智能客服助手，请问有什么可以帮助您的？',
        NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO ca_tenant (tenant_id, name, industry, status, welcome_message)
VALUES ('shop123', '示例电商', 'ecommerce', 'ACTIVE',
        '欢迎光临！我是您的专属购物助手，可以帮您查询订单、推荐商品、处理售后~')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO ca_tenant (tenant_id, name, industry, status, welcome_message)
VALUES ('bank456', '示例金融', 'finance', 'ACTIVE',
        '您好，我是金融智能助手。为了保障您的账户安全，请勿透露密码等敏感信息。')
ON DUPLICATE KEY UPDATE name = VALUES(name);
