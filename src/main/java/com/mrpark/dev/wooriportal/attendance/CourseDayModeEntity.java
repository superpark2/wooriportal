package com.mrpark.dev.wooriportal.attendance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 학생별 당일 로그인 모드 (퇴실 불필요 여부)
 * - "QR"     : QR 로그인
 * - "BEACON" : 비컨 로그인
 */
@Entity
@Table(name = "student_day_mode",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "date"}))
@Getter @Setter
public class CourseDayModeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    /** QR_IN / QR_OUT / BEACON_IN / BEACON_OUT */
    @Column(name = "mode", nullable = false, length = 10)
    private String mode;

    /** 수동 입력 입실 시간 ("HH:mm") */
    @Column(name = "check_in", length = 5)
    private String checkIn;

    /** 수동 입력 퇴실 시간 ("HH:mm") */
    @Column(name = "check_out", length = 5)
    private String checkOut;
}
