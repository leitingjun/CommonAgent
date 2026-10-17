package com.commonagent.service;

import com.commonagent.dto.TenantDTO;
import com.commonagent.model.Tenant;
import com.commonagent.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 租户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantAdminService {

    private final TenantRepository tenantRepository;
    private final KnowledgeService knowledgeService;

    /**
     * 注册新租户
     */
    @Transactional
    public Tenant registerTenant(TenantDTO dto) {
        if (tenantRepository.existsByTenantId(dto.getTenantId())) {
            throw new IllegalArgumentException("租户ID已存在: " + dto.getTenantId());
        }

        Tenant tenant = Tenant.builder()
                .tenantId(dto.getTenantId())
                .name(dto.getName())
                .industry(dto.getIndustry() != null ? dto.getIndustry() : "general")
                .contactEmail(dto.getContactEmail())
                .welcomeMessage(dto.getWelcomeMessage())
                .promptTemplate(dto.getPromptTemplate())
                .maxConcurrentSessions(dto.getMaxConcurrentSessions() != null ? dto.getMaxConcurrentSessions() : 100)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        tenant = tenantRepository.save(tenant);

        // 为租户创建 ES 知识库索引
        knowledgeService.ensureTenantIndex(dto.getTenantId());

        log.info("[TenantAdmin] Registered tenant: {} ({})", dto.getTenantId(), dto.getName());
        return tenant;
    }

    /**
     * 获取所有活跃租户
     */
    public List<Tenant> getActiveTenants() {
        return tenantRepository.findAll();
    }

    /**
     * 更新租户配置
     */
    @Transactional
    public Tenant updateTenant(String tenantId, TenantDTO dto) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("租户不存在: " + tenantId));

        if (dto.getName() != null) tenant.setName(dto.getName());
        if (dto.getIndustry() != null) tenant.setIndustry(dto.getIndustry());
        if (dto.getContactEmail() != null) tenant.setContactEmail(dto.getContactEmail());
        if (dto.getWelcomeMessage() != null) tenant.setWelcomeMessage(dto.getWelcomeMessage());
        if (dto.getPromptTemplate() != null) tenant.setPromptTemplate(dto.getPromptTemplate());
        if (dto.getMaxConcurrentSessions() != null) tenant.setMaxConcurrentSessions(dto.getMaxConcurrentSessions());

        return tenantRepository.save(tenant);
    }

    /**
     * 停用租户
     */
    @Transactional
    public void deactivateTenant(String tenantId) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("租户不存在: " + tenantId));
        tenant.setStatus("INACTIVE");
        tenantRepository.save(tenant);
        log.info("[TenantAdmin] Deactivated tenant: {}", tenantId);
    }
}
