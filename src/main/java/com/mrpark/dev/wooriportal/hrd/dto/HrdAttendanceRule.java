package com.mrpark.dev.wooriportal.hrd.dto;

import java.time.LocalTime;

/**
 * 출결/퇴실 판정 규칙.
 *
 * <p>HRD 의 atendSttusNm(하루 지나면 전원 결석)은 무시하고 <b>입실/퇴실 시각</b>으로 판정한다.
 * 단 훈련생 상태가 '중도탈락'이면 그대로 중도탈락으로 표기한다.</p>
 *
 * <h3>출석 판정(강의일)</h3>
 * <ul>
 *   <li>입실 ≤ 시작+{@value #GRACE_MIN}분 → 출석, 그 후 ~ 50%지점 → 지각, 그 후/미입실(50%경과) → 결석</li>
 *   <li>50%지점 = 순수 훈련시간의 50%(점심 1시간 제외). 예) 09:00~18:00 → 14:00</li>
 * </ul>
 * <h3>퇴실 판정(강의일)</h3>
 * <ul>
 *   <li>퇴실시각 &lt; 종료 → 조퇴, ≥ 종료 → 정상퇴실</li>
 *   <li>종료 지났는데 미퇴실 → 미퇴실(경고)</li>
 * </ul>
 */
public final class HrdAttendanceRule {

    public static final String PRESENT = "출석";
    public static final String LATE = "지각";
    public static final String ABSENT = "결석";
    public static final String WAITING = "미출석";
    public static final String DROPPED = "중도탈락";

    public static final String CHECKOUT_DONE = "퇴실";
    public static final String EARLY_LEAVE = "조퇴";
    public static final String NOT_CHECKED_OUT = "미퇴실";

    public static final int GRACE_MIN = 10;
    private static final int LUNCH_START = 12 * 60; // 12:00
    private static final int LUNCH_END = 13 * 60;   // 13:00
    private static final int DEFAULT_HALF_MIN = 240;

    private HrdAttendanceRule() {
    }

    /** 출석 상태(출석/지각/결석/미출석/중도탈락). */
    public static String evaluate(boolean classDay, String checkInHHmm, String beginHHmm,
                                  String endHHmm, LocalTime now, String trneeStatus) {
        if (trneeStatus != null && trneeStatus.contains("탈락")) {
            return DROPPED;
        }
        if (!classDay) {
            return WAITING;
        }
        boolean checkedIn = notBlank(checkInHHmm);
        Integer start = toMin(beginHHmm);
        if (start == null) {
            return checkedIn ? PRESENT : WAITING;
        }
        int half = halfPointClock(start, toMin(endHHmm));
        int grace = start + GRACE_MIN;

        if (checkedIn) {
            Integer t = toMin(checkInHHmm);
            if (t == null || t <= grace) {
                return PRESENT;
            }
            return t <= half ? LATE : ABSENT;
        }
        return nowMin(now) <= half ? WAITING : ABSENT;
    }

    /** 퇴실 상태(퇴실/조퇴/미퇴실/null). null = 아직 수업중 등 표기 불필요. */
    public static String evaluateCheckout(boolean classDay, String levromHHmm, String endHHmm, LocalTime now) {
        if (!classDay) {
            return null;
        }
        Integer end = toMin(endHHmm);
        if (notBlank(levromHHmm)) {
            Integer t = toMin(levromHHmm);
            if (end != null && t != null && t < end) {
                return EARLY_LEAVE;
            }
            return CHECKOUT_DONE;
        }
        if (end != null && nowMin(now) > end) {
            return NOT_CHECKED_OUT;
        }
        return null;
    }

    /** 순수 훈련시간(점심 제외) 50% 지점의 시계시각(분). */
    static int halfPointClock(int start, Integer endNullable) {
        if (endNullable == null || endNullable <= start) {
            return start + DEFAULT_HALF_MIN;
        }
        int end = endNullable;
        int duration = end - start;
        // 점심 1시간 제외: 12:00~13:00 을 가로지르고 총 6시간 이상인 종일과정만
        boolean hasLunch = start <= LUNCH_START && end >= LUNCH_END && duration >= 360;
        int training = duration - (hasLunch ? 60 : 0);
        int half = training / 2;
        if (!hasLunch) {
            return start + half;
        }
        int morningTraining = LUNCH_START - start; // 점심 전 훈련량
        if (half <= morningTraining) {
            return start + half;
        }
        return LUNCH_END + (half - morningTraining); // 점심 후로 넘어감
    }

    private static int nowMin(LocalTime now) {
        return now.getHour() * 60 + now.getMinute();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    static Integer toMin(String hhmm) {
        if (hhmm == null || hhmm.length() != 4) {
            return null;
        }
        try {
            return Integer.parseInt(hhmm.substring(0, 2)) * 60 + Integer.parseInt(hhmm.substring(2, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
