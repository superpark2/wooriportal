package com.mrpark.dev.wooriportal.attendance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "student")
@Getter @Setter
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    /** 학생 이름 */
    @Column(name = "student_name", nullable = false, length = 50)
    private String studentName;

    /**
     * 생년월일 앞자리 (선택 — 동명 2인 구분용)
     * 예) "990101"
     */
    @Column(name = "birth_prefix", length = 10)
    private String birthPrefix;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
