package com.heartify.aiservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "explanations")
public class Explanation {

    @Id
    private String id;

    @Field("llm_model_version")
    private Integer llmModelVersion;

    @Field("prompt")
    private Map<String, Object> prompt;

    @Field("explanation")
    private Map<String, Object> explanation;

    @Field("created_at")
    private LocalDateTime createdAt;

    public Explanation() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
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
        private final Explanation explanation;

        private Builder() {
            explanation = new Explanation();
        }

        public Builder id(String id) {
            explanation.id = id;
            return this;
        }

        public Builder llmModelVersion(Integer llmModelVersion) {
            explanation.llmModelVersion = llmModelVersion;
            return this;
        }

        public Builder prompt(Map<String, Object> prompt) {
            explanation.prompt = prompt;
            return this;
        }

        public Builder explanation(Map<String, Object> explanation) {
            this.explanation.explanation = explanation;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            explanation.createdAt = createdAt;
            return this;
        }

        public Explanation build() {
            return explanation;
        }
    }
}
