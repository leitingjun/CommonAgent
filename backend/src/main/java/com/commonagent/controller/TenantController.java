package com.commonagent.controller;

import com.commonagent.dto.TenantDTO;
import com.commonagent.model.Tenant;
import com.commonagent.service.TenantAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 租户管理控制器
 */
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantAdminService tenantAdminService;

    /**
     * 注册新租户 (开放接口，无需 X-Tenant-Id)
     */
    @PostMapping("/register")
    public ResponseEntity<Tenant> register(@Valid @RequestBody TenantDTO dto) {
        Tenant tenant = tenantAdminService.registerTenant(dto);
        return ResponseEntity.ok(tenant);
    }

    /**
     * 获取所有租户 (管理端)
     */
    @GetMapping
    public ResponseEntity<List<Tenant>> listTenants() {
        return ResponseEntity.ok(tenantAdminService.getActiveTenants());
    }

    /**
     * 获取租户详情
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<Tenant> getTenant(@PathVariable String tenantId) {
        // 简化实现
        return ResponseEntity.ok(null);
    }

    /**
     * 更新租户配置
     */
    @PutMapping("/{tenantId}")
    public ResponseEntity<Tenant> updateTenant(
            @PathVariable String tenantId,
            @Valid @RequestBody TenantDTO dto) {
        Tenant tenant = tenantAdminService.updateTenant(tenantId, dto);
        return ResponseEntity.ok(tenant);
    }

    /**
     * 停用租户
     */
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Map<String, String>> deactivate(@PathVariable String tenantId) {
        tenantAdminService.deactivateTenant(tenantId);
        return ResponseEntity.ok(Map.of("message", "租户已停用"));
    }
}
