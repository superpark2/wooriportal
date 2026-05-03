package com.park.welstory.wooriportal.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AIDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaRequest {
        private String model;
        private List<Message> messages;
        private boolean stream;
        private String format;
        private Options options;
        private Boolean think; // ← 여기로 이동

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Options {
            @JsonProperty("num_predict")
            private Integer numPredict;

            @JsonProperty("num_ctx")
            private Integer numCtx;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Message {
            private String role;
            private String content;
            private List<String> images;
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OllamaResponse {
        private Message message;
        private boolean done;

        @Data
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Message {
            private String role;
            private String content;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        private List<OllamaRequest.Message> messages;
        private String sessionId;
        private String editImageUrl;
        private String skin;
    }
}