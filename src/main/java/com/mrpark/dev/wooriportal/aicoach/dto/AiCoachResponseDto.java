package com.mrpark.dev.wooriportal.aicoach.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrpark.dev.wooriportal.aicoach.entity.AICoachAnswer;
import com.mrpark.dev.wooriportal.aicoach.entity.AICoachQuestion;
import com.mrpark.dev.wooriportal.aicoach.entity.AICoachSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class AiCoachResponseDto {

    /** 공통 API 래퍼 */
    @Getter @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String error;

        public static <T> ApiResponse<T> ok(T data)       { return new ApiResponse<>(true,  data, null); }
        public static <T> ApiResponse<T> fail(String msg) { return new ApiResponse<>(false, null, msg);  }
    }

    @Getter @Builder
    public static class SessionResponse {
        private Long id;
        private String job;
        private String company;
        @JsonProperty("question_count")
        private Integer questionCount;
        @JsonProperty("user_id")
        private Long userId;
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
        @JsonProperty("avg_score")
        private Integer avgScore;

        public static SessionResponse from(AICoachSession s, Integer avgScore) {
            return SessionResponse.builder()
                    .id(s.getId()).job(s.getJob()).company(s.getCompany())
                    .questionCount(s.getQuestionCount()).userId(s.getUserId())
                    .createdAt(s.getCreatedAt()).avgScore(avgScore)
                    .build();
        }
    }

    @Getter @Builder
    public static class QuestionResponse {
        private Long id;
        @JsonProperty("session_id")
        private Long sessionId;
        @JsonProperty("question_text")
        private String questionText;
        @JsonProperty("order_num")
        private Integer orderNum;
        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        public static QuestionResponse from(AICoachQuestion q) {
            return QuestionResponse.builder()
                    .id(q.getId()).sessionId(q.getSession().getId())
                    .questionText(q.getQuestionText()).orderNum(q.getOrderNum())
                    .createdAt(q.getCreatedAt())
                    .build();
        }
    }

    @Getter @Builder
    public static class AnswerResponse {
        private Long id;
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
        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        public static AnswerResponse from(AICoachAnswer a) {
            return AnswerResponse.builder()
                    .id(a.getId()).questionId(a.getQuestion().getId())
                    .answerText(a.getAnswerText()).feedbackGood(a.getFeedbackGood())
                    .feedbackImprove(a.getFeedbackImprove()).score(a.getScore())
                    .timeTaken(a.getTimeTaken()).starScore(a.getStarScore())
                    .relevanceScore(a.getRelevanceScore()).detailScore(a.getDetailScore())
                    .createdAt(a.getCreatedAt())
                    .build();
        }
    }

    @Getter @Builder
    public static class DashboardResponse {
        @JsonProperty("total_sessions")
        private long totalSessions;
        @JsonProperty("avg_score")
        private int avgScore;
        @JsonProperty("best_score")
        private int bestScore;
        @JsonProperty("best_label")
        private String bestLabel;
        @JsonProperty("month_sessions")
        private long monthSessions;
        @JsonProperty("recent_sessions")
        private List<RecentSessionDto> recentSessions;
    }

    @Getter @Builder
    public static class RecentSessionDto {
        private Long id;
        private String job;
        private String company;
        @JsonProperty("avg_score")
        private int avgScore;
        private String date;
    }
}
