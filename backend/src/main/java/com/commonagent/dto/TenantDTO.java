package com.commonagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDTO {
    @NotBlank
    private String tenantId;
    @NotBlank
    private String name;
    private String industry;
    private String contactEmail;
    private String welcomeMessage;
    private String promptTemplate;
    private Integer maxConcurrentSessions;
}
