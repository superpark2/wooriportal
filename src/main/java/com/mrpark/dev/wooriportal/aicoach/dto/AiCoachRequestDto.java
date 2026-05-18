package com.mrpark.dev.wooriportal.aicoach.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class AiCoachRequestDto {

    @Getter @NoArgsConstructor
    public static class SessionCreateRequest {
        private String job;
        private String company;
        @JsonProperty("question_count")
        private Integer questionCount;
        @JsonProperty("user_id")
        private Long userId;
    }

    @Getter @NoArgsConstructor
    public static class QuestionSaveRequest {
        @JsonProperty("session_id")
        private Long sessionId;
        private List<String> questions;
    }

    @Getter @NoArgsConstructor
    public static class AnswerSaveRequest {
        @JsonProperty("question_id")
        private Long questionId;
        @JsonProperty("answer_text")
        private String answerText;
        @JsonProperty("feedback_good")
        private String feedbackGood;
        @JsonProperty("feedback_improve")
        private String feedbackImprove;
        private Integer score;
        @JsonProperty("time_taken")
        private Integer timeTaken;
        @JsonProperty("star_score")
        private Integer starScore;
        @JsonProperty("relevance_score")
        private Integer relevanceScore;
        @JsonProperty("detail_score")
        private Integer detailScore;
    }

    @Getter @NoArgsConstructor
    public static class GenerateQuestionsRequest {
        private String job;
        private String company;
        @JsonProperty("question_count")
        private Integer questionCount;
        @JsonProperty("interview_type")
        private String interviewType;
    }

    @Getter @NoArgsConstructor
    public static class GenerateFeedbackRequest {
        private String job;
        private String company;
        private String question;
        private String answer;
    }

    @Getter @NoArgsConstructor
    public static class SpellCheckRequest {
        private String content;
    }

    @Getter @NoArgsConstructor
    public static class ResumeFeedbackRequest {
        private String content;
        private String company;
        private String job;
    }

    @Getter @NoArgsConstructor
    public static class ExtractImageRequest {
        private String image;
    }
}
