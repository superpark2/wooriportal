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
 * 전광판 1과정 카드(브라우저 전용 뷰). 출결/퇴실은 {@link HrdAttendanceRule} 로 재판정한다.
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
    private final int dropped;        // 중도탈락
    private final int checkedOut;     // 퇴실(정상)
    private final int earlyLeave;     // 조퇴
    private final int notCheckedOut;  // 미퇴실(경고)
    private final boolean allCheckedOut;

    private final boolean classDay;
    private final List<Integer> scheduledDays;
    private final String notes;       // 특이사항(공유)

    private final List<Attendee> attendees;

    public HrdBoardRow(HrdDailyAttendance a, boolean classDay, LocalTime now,
                       List<Integer> scheduledDays, String notes) {
        HrdCourseDetail c = a.getCourse();
        this.tracseId = c != null ? c.getTracseId() : null;
        this.tracseTme = c != null ? c.getTracseTme() : null;
        this.courseName = c != null ? c.getTracseNm() : null;
        this.period = c != null ? c.getTracsePd() : null;
        this.time = c != null ? c.getTraingTime() : null;
        this.classDay = classDay;
        this.scheduledDays = scheduledDays != null ? scheduledDays : List.of();
        this.notes = notes;

        String begin = c != null ? c.getTraingBeginTime() : null;
        String end = c != null ? c.getTraingEndTime() : null;

        int p = 0, l = 0, ab = 0, w = 0, drop = 0, out = 0, early = 0, noOut = 0;
        List<Attendee> list = new ArrayList<>();
        for (HrdAttendee at : a.getRoster()) {
            String st = HrdAttendanceRule.evaluate(classDay, at.getCheckInTime(), begin, end, now, at.getTrneeSttusNm());
            at.setComputedStatus(st);
            String co = HrdAttendanceRule.evaluateCheckout(classDay, at.getCheckOutTime(), end, now);
            at.setCheckoutStatus(co);

            switch (st) {
                case HrdAttendanceRule.PRESENT -> p++;
                case HrdAttendanceRule.LATE -> l++;
                case HrdAttendanceRule.ABSENT -> ab++;
                case HrdAttendanceRule.DROPPED -> drop++;
                default -> w++;
            }
            if (HrdAttendanceRule.EARLY_LEAVE.equals(co)) {
                early++;
                out++;
            } else if (HrdAttendanceRule.CHECKOUT_DONE.equals(co)) {
                out++;
            } else if (HrdAttendanceRule.NOT_CHECKED_OUT.equals(co)) {
                noOut++;
            }
            list.add(new Attendee(at));
        }
        this.total = a.getRoster().size();
        this.present = p;
        this.late = l;
        this.absent = ab;
        this.waiting = w;
        this.dropped = drop;
        this.checkedOut = out;
        this.earlyLeave = early;
        this.notCheckedOut = noOut;
        int active = total - drop;
        this.allCheckedOut = active > 0 && out >= active;
        this.attendees = list;
    }

    /** 브라우저 표시용 최소 수강생 정보(주민번호 제외). */
    @Getter
    public static class Attendee {
        private final String name;
        private final String type;
        private final String status;          // 출석/지각/결석/미출석/중도탈락
        private final String checkoutStatus;  // 퇴실/조퇴/미퇴실/null
        private final String checkInTime;
        private final String checkOutTime;
        private final String telno;
        private final boolean checkedOut;

        Attendee(HrdAttendee a) {
            this.name = a.getCstmrNm();
            this.type = a.getTrneeSeNm();
            this.status = a.getComputedStatus();
            this.checkoutStatus = a.getCheckoutStatus();
            this.checkInTime = a.getCheckInTime();
            this.checkOutTime = a.getCheckOutTime();
            this.telno = a.getTelno();
            this.checkedOut = a.isCheckedOut();
        }
    }
}
