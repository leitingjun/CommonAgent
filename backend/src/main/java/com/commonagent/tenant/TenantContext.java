package com.commonagent.tenant;

/**
 * 租户上下文 - 使用 ThreadLocal 在当前请求线程中传递租户信息
 */
public class TenantContext {

    private static final ThreadLocal<TenantInfo> TENANT_HOLDER = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenant(TenantInfo tenantInfo) {
        TENANT_HOLDER.set(tenantInfo);
    }

    public static TenantInfo getTenant() {
        return TENANT_HOLDER.get();
    }

    public static String getTenantId() {
        TenantInfo info = TENANT_HOLDER.get();
        return info != null ? info.tenantId() : null;
    }

    public static void clear() {
        TENANT_HOLDER.remove();
    }

    /**
     * 租户信息
     */
    public record TenantInfo(String tenantId, String tenantName, String industry) {}
}
