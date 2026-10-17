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
public class KnowledgeDTO {
    private String knowledgeId;
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private String category;
    private String tags;
    private String status;
    private String sourceType;
}
