package com.maihehe.blogcore._06_DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TreeNodeVisibilityUpdateRequest {
    @NotNull private NodeType nodeType;   // "TOPIC" or "NOTE"
    @NotBlank private String path;
    @NotNull private Boolean visible;

    public static enum NodeType {
        TOPIC,
        NOTE
    }
}

