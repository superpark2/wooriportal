package com.mrpark.dev.wooriportal.attendance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_log")
@Getter @Setter
public class AttendanceLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 카드번호 (더 이상 사용 안 함 — 하위 호환을 위해 nullable 유지) */
    @Column(name = "card_num", nullable = true, length = 30)
    private String cardNum;

    /** 학생 이름 */
    @Column(name = "student_name", nullable = false, length = 50)
    private String studentName;

    /** 과정명 */
    @Column(name = "course_name", nullable = false, length = 500)
    private String courseName;

    /** 입실 시간 (HHMM 4자리, e.g. "1202") */
    @Column(name = "check_in", length = 4)
    private String checkIn;

    /** 퇴실 시간 (HHMM 4자리, e.g. "1250" / null = 아직 퇴실 안함) */
    @Column(name = "check_out", length = 4)
    private String checkOut;

    /** 출결 날짜 */
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
