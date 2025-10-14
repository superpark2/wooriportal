package com.park.welstory.wooriportal.common.utilities.ai;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AiDTO {
    // 요청용 DTO
    @Data
    @NoArgsConstructor
    public static class GeminiRequest {
        public List<Content> contents;
        @Data
        @NoArgsConstructor
        public static class Content {
            public String role;
            public List<Part> parts;
            @Data
            @NoArgsConstructor
            public static class Part {
                public String text;
            }
        }
    }

    // 응답용 DTO
    @Data
    @NoArgsConstructor
    public static class GeminiResponse {
        public List<Candidate> candidates;
        @Data
        @NoArgsConstructor
        public static class Candidate {
            public Content content;
            @Data
            @NoArgsConstructor
            public static class Content {
                public List<Part> parts;
                @Data
                @NoArgsConstructor
                public static class Part {
                    public String text;
                }
            }
        }
    }
}