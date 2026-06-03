package com.mrpark.dev.wooriportal.hrd.dto;

import java.time.LocalTime;

/**
 * 출결 판정 규칙.
 *
 * <p>HRD 의 atendSttusNm(하루 지나면 전원 결석으로 뜸)은 <b>무시</b>하고, <b>입실시각(lpsilTime)</b>과
 * 강의 시작/종료 시각으로 직접 판정한다.</p>
 *
 * <ul>
 *   <li>입실 ≤ 시작+{@value #GRACE_MIN}분  → 출석</li>
 *   <li>시작+유예 &lt; 입실 ≤ 50%지점       → 지각</li>
 *   <li>입실 &gt; 50%지점                    → 결석(너무 늦게 입실)</li>
 *   <li>입실 없음 + 현재 ≤ 50%지점          → 미출석(아직 대기)</li>
 *   <li>입실 없음 + 현재 &gt; 50%지점        → 결석</li>
 *   <li>강의일(요일) 아님                    → 미출석</li>
 * </ul>
 */
public final class HrdAttendanceRule {

    public static final String PRESENT = "출석";
    public static final String LATE = "지각";
    public static final String ABSENT = "결석";
    public static final String WAITING = "미출석";

    /** 정시 출석 유예(분). 09:00 강의면 09:10 까지 입실은 출석. */
    public static final int GRACE_MIN = 10;
    /** 시작/종료 시각을 모를 때 50% 지점 기본값(분). */
    private static final int DEFAULT_HALF_MIN = 240;

    private HrdAttendanceRule() {
    }

    /**
     * @param classDay    오늘이 이 과정의 강의 요일인가
     * @param checkInHHmm 입실시각 HHmm(없으면 null/공백)
     * @param beginHHmm   강의 시작 HHmm
     * @param endHHmm     강의 종료 HHmm
     * @param now         현재 시각
     */
    public static String evaluate(boolean classDay, String checkInHHmm,
                                  String beginHHmm, String endHHmm, LocalTime now) {
        if (!classDay) {
            return WAITING;
        }
        boolean checkedIn = checkInHHmm != null && !checkInHHmm.isBlank();
        Integer start = toMin(beginHHmm);
        if (start == null) {
            // 시각 정보 없음 → 입실 여부만으로
            return checkedIn ? PRESENT : WAITING;
        }
        Integer end = toMin(endHHmm);
        int halfPoint = (end != null && end > start) ? start + (end - start) / 2 : start + DEFAULT_HALF_MIN;
        int grace = start + GRACE_MIN;

        if (checkedIn) {
            Integer t = toMin(checkInHHmm);
            if (t == null) {
                return PRESENT;
            }
            if (t <= grace) {
                return PRESENT;
            }
            if (t <= halfPoint) {
                return LATE;
            }
            return ABSENT;
        }
        int nowMin = now.getHour() * 60 + now.getMinute();
        return nowMin <= halfPoint ? WAITING : ABSENT;
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
