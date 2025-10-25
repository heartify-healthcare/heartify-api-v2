package com.heartify.aiservice.dto;

public class MessageResponse {
    private String message;

    public MessageResponse() {
    }

    public MessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final MessageResponse response;

        private Builder() {
            response = new MessageResponse();
        }

        public Builder message(String message) {
            response.message = message;
            return this;
        }

        public MessageResponse build() {
            return response;
        }
    }
}
