package com.mrpark.dev.wooriportal.hrd.board;

import com.mrpark.dev.wooriportal.hrd.dto.HrdAttendanceRule;
import com.mrpark.dev.wooriportal.hrd.dto.HrdAttendee;
import com.mrpark.dev.wooriportal.hrd.dto.HrdCourseDetail;
import com.mrpark.dev.wooriportal.hrd.dto.HrdDailyAttendance;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * 전광판 1과정 카드(브라우저 전용 뷰). 출결은 {@link HrdAttendanceRule} 로 재판정한다.
 */
@Getter
public class HrdBoardRow {

    private final String tracseId;
    private final String tracseTme;
    private final String courseName;
    private final String period;
    private final String time;

    private final int total;
    private final int present;
    private final int late;
    private final int absent;
    private final int waiting;
    private final int checkedOut;
    private final boolean allCheckedOut;

    /** 오늘이 이 과정 강의요일인가 */
    private final boolean classDay;
    /** 설정된 강의요일(1=월..7=일). 미설정이면 빈 리스트. */
    private final List<Integer> scheduledDays;

    private final List<Attendee> attendees;

    public HrdBoardRow(HrdDailyAttendance a, boolean classDay, LocalTime now, List<Integer> scheduledDays) {
        HrdCourseDetail c = a.getCourse();
        this.tracseId = c != null ? c.getTracseId() : null;
        this.tracseTme = c != null ? c.getTracseTme() : null;
        this.courseName = c != null ? c.getTracseNm() : null;
        this.period = c != null ? c.getTracsePd() : null;
        this.time = c != null ? c.getTraingTime() : null;
        this.classDay = classDay;
        this.scheduledDays = scheduledDays != null ? scheduledDays : List.of();

        String begin = c != null ? c.getTraingBeginTime() : null;
        String end = c != null ? c.getTraingEndTime() : null;

        int p = 0, l = 0, ab = 0, w = 0, out = 0;
        List<Attendee> list = new ArrayList<>();
        for (HrdAttendee at : a.getRoster()) {
            String st = HrdAttendanceRule.evaluate(classDay, at.getCheckInTime(), begin, end, now);
            at.setComputedStatus(st);
            switch (st) {
                case HrdAttendanceRule.PRESENT -> p++;
                case HrdAttendanceRule.LATE -> l++;
                case HrdAttendanceRule.ABSENT -> ab++;
                default -> w++;
            }
            if (at.isCheckedOut()) {
                out++;
            }
            list.add(new Attendee(at));
        }
        this.total = a.getRoster().size();
        this.present = p;
        this.late = l;
        this.absent = ab;
        this.waiting = w;
        this.checkedOut = out;
        this.allCheckedOut = total > 0 && out >= total;
        this.attendees = list;
    }

    /** 브라우저 표시용 최소 수강생 정보(주민번호 제외). */
    @Getter
    public static class Attendee {
        private final String name;
        private final String type;
        private final String status;       // 재판정 상태
        private final String checkInTime;
        private final String checkOutTime;
        private final boolean checkedOut;

        Attendee(HrdAttendee a) {
            this.name = a.getCstmrNm();
            this.type = a.getTrneeSeNm();
            this.status = a.getComputedStatus();
            this.checkInTime = a.getCheckInTime();
            this.checkOutTime = a.getCheckOutTime();
            this.checkedOut = a.isCheckedOut();
        }
    }
}
