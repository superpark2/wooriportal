package com.mrpark.dev.wooriportal.hrd.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/** 출결 재판정 규칙(입실시각 기준, 점심 제외 50%, 중도탈락, 퇴실/조퇴/미퇴실). */
class HrdAttendanceRuleTest {

    // 09:00~18:00 = 점심1h 제외 8h, 50% 지점 = 14:00
    private String at(String checkIn, LocalTime now) {
        return HrdAttendanceRule.evaluate(true, checkIn, "0900", "1800", now, null, null);
    }

    @Test
    void withinGraceIsPresent() {
        assertThat(at("0900", LocalTime.of(9, 0))).isEqualTo(HrdAttendanceRule.PRESENT);
        assertThat(at("0910", LocalTime.of(9, 30))).isEqualTo(HrdAttendanceRule.PRESENT);
    }

    @Test
    void afterGraceUntilHalfIsLate() {
        assertThat(at("0911", LocalTime.of(9, 30))).isEqualTo(HrdAttendanceRule.LATE);
        assertThat(at("1400", LocalTime.of(14, 5))).isEqualTo(HrdAttendanceRule.LATE);   // 50% 경계(점심 제외 → 14:00)
    }

    @Test
    void afterHalfIsAbsent() {
        assertThat(at("1401", LocalTime.of(14, 10))).isEqualTo(HrdAttendanceRule.ABSENT);
        assertThat(at(null, LocalTime.of(14, 1))).isEqualTo(HrdAttendanceRule.ABSENT);
    }

    @Test
    void noCheckInBeforeHalfIsWaiting() {
        assertThat(at(null, LocalTime.of(13, 0))).isEqualTo(HrdAttendanceRule.WAITING);
    }

    @Test
    void fourHourCourseNoLunch() {
        // 09:00~13:00 (4h, 점심 없음) → 50% = 11:00
        assertThat(HrdAttendanceRule.evaluate(true, "1059", "0900", "1300", LocalTime.of(11, 30), null, null))
                .isEqualTo(HrdAttendanceRule.LATE);
        assertThat(HrdAttendanceRule.evaluate(true, "1101", "0900", "1300", LocalTime.of(11, 30), null, null))
                .isEqualTo(HrdAttendanceRule.ABSENT);
    }

    @Test
    void droppedAlwaysDropped() {
        assertThat(HrdAttendanceRule.evaluate(true, "0900", "0900", "1800", LocalTime.of(9, 0), "중도탈락", null))
                .isEqualTo(HrdAttendanceRule.DROPPED);
    }

    @Test
    void configurableLunch() {
        // 09:00~18:00 점심 0분 → 50% = 13:30
        assertThat(HrdAttendanceRule.evaluate(true, "1325", "0900", "1800", LocalTime.of(13, 40), null, 0))
                .isEqualTo(HrdAttendanceRule.LATE);
        assertThat(HrdAttendanceRule.evaluate(true, "1335", "0900", "1800", LocalTime.of(13, 40), null, 0))
                .isEqualTo(HrdAttendanceRule.ABSENT);
        // 점심 120분 → 50% = 14:30
        assertThat(HrdAttendanceRule.evaluate(true, "1425", "0900", "1800", LocalTime.of(14, 40), null, 120))
                .isEqualTo(HrdAttendanceRule.LATE);
    }

    @Test
    void nonClassDayIsWaiting() {
        assertThat(HrdAttendanceRule.evaluate(false, "0900", "0900", "1800", LocalTime.of(9, 0), null, null))
                .isEqualTo(HrdAttendanceRule.WAITING);
    }

    @Test
    void checkout() {
        // 18:00 종료, 종료 10분전(17:50)부터 정상 퇴실
        assertThat(HrdAttendanceRule.evaluateCheckout(true, "1749", "1800", LocalTime.of(17, 55)))
                .isEqualTo(HrdAttendanceRule.EARLY_LEAVE);            // 17:49 < 17:50 → 조퇴
        assertThat(HrdAttendanceRule.evaluateCheckout(true, "1750", "1800", LocalTime.of(17, 55)))
                .isEqualTo(HrdAttendanceRule.CHECKOUT_DONE);          // 17:50 → 정상
        assertThat(HrdAttendanceRule.evaluateCheckout(true, "1800", "1800", LocalTime.of(18, 5)))
                .isEqualTo(HrdAttendanceRule.CHECKOUT_DONE);          // 정상
        assertThat(HrdAttendanceRule.evaluateCheckout(true, null, "1800", LocalTime.of(18, 30)))
                .isEqualTo(HrdAttendanceRule.NOT_CHECKED_OUT);        // 미퇴실
        assertThat(HrdAttendanceRule.evaluateCheckout(true, null, "1800", LocalTime.of(15, 0)))
                .isNull();                                           // 수업중
    }
}
