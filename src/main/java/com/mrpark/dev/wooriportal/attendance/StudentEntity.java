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

    /** HRD 카드번호 (마스킹 포함, null 가능 → 이름 매칭으로 대체) */
    @Column(name = "card_num", length = 30)
    private String cardNum;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
