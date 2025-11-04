package com.example.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request moderation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationRequest {

    @NotBlank(message = "ID không được để trống")
    private String id;

    @NotBlank(message = "Text không được để trống")
    private String text;

    private String runId; // Optional: để nhóm các request trong cùng một test run
}
