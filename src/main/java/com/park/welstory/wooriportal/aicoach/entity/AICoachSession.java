package com.park.welstory.wooriportal.aicoach.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "aicoachsessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AICoachSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String job;

    @Column(nullable = false)
    private String company;

    @Column(name = "question_count")
    private Integer questionCount;

    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AICoachQuestion> questions = new ArrayList<>();
}
