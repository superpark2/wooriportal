package com.mrpark.dev.wooriportal.hrd.dto;

import java.util.List;
import lombok.Getter;

/**
 * 한 과정의 당일 출결 스냅샷(전광판 1카드 분량): 과정 헤더 + 수강생 출결 + 집계.
 */
@Getter
public class HrdDailyAttendance {

    private final HrdCourseDetail course;
    private final List<HrdAttendee> roster;

    private final int total;
    private final int present;     // 출석(입실 완료)
    private final int late;        // 지각
    private final int absent;      // 결석
    private final int waiting;     // 미입력(아직 안 찍힘, 상태 "-"/빈값)
    private final int checkedOut;  // 퇴실 완료

    public HrdDailyAttendance(HrdCourseDetail course, List<HrdAttendee> roster) {
        this.course = course;
        this.roster = roster;

        int p = 0, l = 0, ab = 0, w = 0, out = 0;
        for (HrdAttendee a : roster) {
            String status = a.getAtendSttusNm();
            if (status != null && status.contains("지각")) {
                l++;
            } else if (status != null && status.contains("결석")) {
                ab++;
            } else if (status != null && status.contains("출석")) {
                p++;
            } else {
                w++; // "-", 빈값, null 등 미입력
            }
            if (a.isCheckedOut()) {
                out++;
            }
        }
        this.total = roster.size();
        this.present = p;
        this.late = l;
        this.absent = ab;
        this.waiting = w;
        this.checkedOut = out;
    }

    /** 모든 수강생이 퇴실 완료(=오늘 일정 종료) → 전광판에서 뒤로 정렬. */
    public boolean isAllCheckedOut() {
        return total > 0 && checkedOut >= total;
    }
}
