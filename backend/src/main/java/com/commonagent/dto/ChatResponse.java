package com.commonagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String conversationId;
    private String messageId;
    private String content;
    private String role;
    private String intent;
    private String sentiment;
    private boolean needHumanHandoff;
    private String handoffReason;
    private LocalDateTime timestamp;
}
