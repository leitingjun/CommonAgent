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
public class ChatRequest {
    @NotBlank(message = "租户ID不能为空")
    private String tenantId;

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    private String conversationId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    /** 消息类型: TEXT, IMAGE */
    @Builder.Default
    private String messageType = "TEXT";

    /** 用户名称 (可选) */
    private String userName;
}
