package com.mrpark.dev.wooriportal.hrd.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/** 출결 재판정 규칙 — 입실시각 기준(HRD 결석상태 무시). 09:00~18:00 강의 기준. */
class HrdAttendanceRuleTest {

    private static final String BEGIN = "0900";
    private static final String END = "1800"; // 50% 지점 = 13:30

    private String eval(String checkIn, LocalTime now) {
        return HrdAttendanceRule.evaluate(true, checkIn, BEGIN, END, now);
    }

    @Test
    void checkInWithinGraceIsPresent() {
        assertThat(eval("0900", LocalTime.of(9, 0))).isEqualTo(HrdAttendanceRule.PRESENT);
        assertThat(eval("0910", LocalTime.of(9, 30))).isEqualTo(HrdAttendanceRule.PRESENT); // 유예 10분 경계
    }

    @Test
    void checkInAfterGraceBeforeHalfIsLate() {
        assertThat(eval("0911", LocalTime.of(9, 30))).isEqualTo(HrdAttendanceRule.LATE);
        assertThat(eval("1330", LocalTime.of(13, 40))).isEqualTo(HrdAttendanceRule.LATE); // 50% 경계
    }

    @Test
    void checkInAfterHalfIsAbsent() {
        assertThat(eval("1400", LocalTime.of(14, 10))).isEqualTo(HrdAttendanceRule.ABSENT);
    }

    @Test
    void noCheckInBeforeHalfIsWaiting() {
        assertThat(eval(null, LocalTime.of(11, 0))).isEqualTo(HrdAttendanceRule.WAITING);
        assertThat(eval("", LocalTime.of(13, 30))).isEqualTo(HrdAttendanceRule.WAITING);
    }

    @Test
    void noCheckInAfterHalfIsAbsent() {
        assertThat(eval(null, LocalTime.of(15, 0))).isEqualTo(HrdAttendanceRule.ABSENT);
    }

    @Test
    void nonClassDayIsAlwaysWaiting() {
        // 입실 찍혀도 강의일 아니면 미출석(결석 아님)
        assertThat(HrdAttendanceRule.evaluate(false, "0900", BEGIN, END, LocalTime.of(9, 0)))
                .isEqualTo(HrdAttendanceRule.WAITING);
        assertThat(HrdAttendanceRule.evaluate(false, null, BEGIN, END, LocalTime.of(15, 0)))
                .isEqualTo(HrdAttendanceRule.WAITING);
    }

    @Test
    void afternoonClassGrace() {
        // 14:00 시작이면 14:10 입실은 출석
        assertThat(HrdAttendanceRule.evaluate(true, "1410", "1400", "1800", LocalTime.of(14, 30)))
                .isEqualTo(HrdAttendanceRule.PRESENT);
        assertThat(HrdAttendanceRule.evaluate(true, "1411", "1400", "1800", LocalTime.of(14, 30)))
                .isEqualTo(HrdAttendanceRule.LATE);
    }
}
