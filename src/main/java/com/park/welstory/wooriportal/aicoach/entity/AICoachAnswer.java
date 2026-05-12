package com.park.welstory.wooriportal.aicoach.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "aicoachanswers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AICoachAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AICoachQuestion question;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "feedback_good", columnDefinition = "TEXT")
    private String feedbackGood;

    @Column(name = "feedback_improve", columnDefinition = "TEXT")
    private String feedbackImprove;

    private Integer score;

    @Column(name = "time_taken")
    private Integer timeTaken;

    @Column(name = "star_score")
    private Integer starScore;

    @Column(name = "relevance_score")
    private Integer relevanceScore;

    @Column(name = "detail_score")
    private Integer detailScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
