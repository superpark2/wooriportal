package com.mrpark.dev.wooriportal.attendance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "course")
@Getter @Setter
public class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 과정명 */
    @Column(name = "course_name", nullable = false, length = 500)
    private String courseName;

    /**
     * 수업 요일 (쉼표 구분, Java DayOfWeek value: 1=월 ~ 7=일)
     * 예: "1,2,3,4,5" = 월~금
     */
    @Column(name = "days_of_week", nullable = false, length = 20)
    private String daysOfWeek;

    /** 과정 시작일 (null = 무제한) */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** 과정 종료일 (null = 무제한) */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** 정상 입실 시간 (HHMM, e.g. "0900") */
    @Column(name = "check_in_time", length = 4)
    private String checkInTime;

    /** 정상 퇴실 시간 (HHMM, e.g. "1800") */
    @Column(name = "check_out_time", length = 4)
    private String checkOutTime;

    /** 지각 유예 시간 (분, 기본 0) */
    @Column(name = "late_grace_minutes")
    private int lateGraceMinutes = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
