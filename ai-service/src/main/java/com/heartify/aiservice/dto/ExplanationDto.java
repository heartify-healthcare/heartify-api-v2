package com.heartify.aiservice.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

public class ExplanationDto {

    private String id;
    private Integer llmModelVersion;
    private Map<String, Object> prompt;
    private Map<String, Object> explanation;
    private LocalDateTime createdAt;

    public ExplanationDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getLlmModelVersion() {
        return llmModelVersion;
    }

    public void setLlmModelVersion(Integer llmModelVersion) {
        this.llmModelVersion = llmModelVersion;
    }

    public Map<String, Object> getPrompt() {
        return prompt;
    }

    public void setPrompt(Map<String, Object> prompt) {
        this.prompt = prompt;
    }

    public Map<String, Object> getExplanation() {
        return explanation;
    }

    public void setExplanation(Map<String, Object> explanation) {
        this.explanation = explanation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ExplanationDto dto;

        private Builder() {
            dto = new ExplanationDto();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public Builder llmModelVersion(Integer llmModelVersion) {
            dto.llmModelVersion = llmModelVersion;
            return this;
        }

        public Builder prompt(Map<String, Object> prompt) {
            dto.prompt = prompt;
            return this;
        }

        public Builder explanation(Map<String, Object> explanation) {
            dto.explanation = explanation;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            dto.createdAt = createdAt;
            return this;
        }

        public ExplanationDto build() {
            return dto;
        }
    }

    public static class CreateExplanationRequest {
        @NotNull(message = "LLM model version is required")
        private Integer llmModelVersion;

        @NotNull(message = "Prompt is required")
        private Map<String, Object> prompt;

        @NotNull(message = "Explanation is required")
        private Map<String, Object> explanation;

        public CreateExplanationRequest() {
        }

        public Integer getLlmModelVersion() {
            return llmModelVersion;
        }

        public void setLlmModelVersion(Integer llmModelVersion) {
            this.llmModelVersion = llmModelVersion;
        }

        public Map<String, Object> getPrompt() {
            return prompt;
        }

        public void setPrompt(Map<String, Object> prompt) {
            this.prompt = prompt;
        }

        public Map<String, Object> getExplanation() {
            return explanation;
        }

        public void setExplanation(Map<String, Object> explanation) {
            this.explanation = explanation;
        }
    }
}
