package com.commonagent.tenant;

import com.commonagent.model.Tenant;
import com.commonagent.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户拦截器 - 从请求头 X-Tenant-Id 解析租户信息并设置上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantRepository tenantRepository;

    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TENANT_HEADER);

        if (!StringUtils.hasText(tenantId)) {
            // 尝试从参数获取
            tenantId = request.getParameter("tenantId");
        }

        if (StringUtils.hasText(tenantId)) {
            Tenant tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
            if (tenant != null && "ACTIVE".equals(tenant.getStatus())) {
                TenantContext.setTenant(new TenantContext.TenantInfo(
                        tenant.getTenantId(),
                        tenant.getName(),
                        tenant.getIndustry()
                ));
                log.debug("[TenantInterceptor] Set tenant: {}", tenantId);
            } else {
                log.warn("[TenantInterceptor] Invalid or inactive tenant: {}", tenantId);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        TenantContext.clear();
    }
}
