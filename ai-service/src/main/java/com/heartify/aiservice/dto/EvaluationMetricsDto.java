package com.heartify.aiservice.dto;

public class EvaluationMetricsDto {

    private Long tUpload;    // Time from client send to server receive (ms)
    private Long tDenoise;   // Time spent in denoising step (ms)
    private Long tClassify;  // Time spent in classification/prediction step (ms)
    private Long tLlm;       // Time spent in LLM explanation step (ms)
    private Long responsedAt; // Server timestamp when response is ready (Unix ms)

    public EvaluationMetricsDto() {
    }

    public Long getTUpload() {
        return tUpload;
    }

    public void setTUpload(Long tUpload) {
        this.tUpload = tUpload;
    }

    public Long getTDenoise() {
        return tDenoise;
    }

    public void setTDenoise(Long tDenoise) {
        this.tDenoise = tDenoise;
    }

    public Long getTClassify() {
        return tClassify;
    }

    public void setTClassify(Long tClassify) {
        this.tClassify = tClassify;
    }

    public Long getTLlm() {
        return tLlm;
    }

    public void setTLlm(Long tLlm) {
        this.tLlm = tLlm;
    }

    public Long getResponsedAt() {
        return responsedAt;
    }

    public void setResponsedAt(Long responsedAt) {
        this.responsedAt = responsedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final EvaluationMetricsDto dto;

        private Builder() {
            dto = new EvaluationMetricsDto();
        }

        public Builder tUpload(Long tUpload) {
            dto.tUpload = tUpload;
            return this;
        }

        public Builder tDenoise(Long tDenoise) {
            dto.tDenoise = tDenoise;
            return this;
        }

        public Builder tClassify(Long tClassify) {
            dto.tClassify = tClassify;
            return this;
        }

        public Builder tLlm(Long tLlm) {
            dto.tLlm = tLlm;
            return this;
        }

        public Builder responsedAt(Long responsedAt) {
            dto.responsedAt = responsedAt;
            return this;
        }

        public EvaluationMetricsDto build() {
            return dto;
        }
    }
}
