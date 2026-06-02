package com.mrpark.dev.wooriportal.hrd.board;

import com.mrpark.dev.wooriportal.hrd.dto.HrdAttendee;
import com.mrpark.dev.wooriportal.hrd.dto.HrdDailyAttendance;
import java.util.List;
import lombok.Getter;

/**
 * 전광판에 보낼 과정 1카드(브라우저로 나가는 뷰 전용 DTO).
 *
 * <p>주민번호 등 민감 식별자는 제외하고 표시에 필요한 값만 담는다.</p>
 */
@Getter
public class HrdBoardRow {

    private final String tracseId;
    private final String tracseTme;
    private final String courseName;
    private final String period;       // 과정 기간 표시
    private final String time;         // 당일 훈련시간 표시

    private final int total;
    private final int present;
    private final int late;
    private final int absent;
    private final int checkedOut;
    private final boolean allCheckedOut;

    private final List<Attendee> attendees;

    public HrdBoardRow(HrdDailyAttendance a) {
        this.tracseId = a.getCourse() != null ? a.getCourse().getTracseId() : null;
        this.tracseTme = a.getCourse() != null ? a.getCourse().getTracseTme() : null;
        this.courseName = a.getCourse() != null ? a.getCourse().getTracseNm() : null;
        this.period = a.getCourse() != null ? a.getCourse().getTracsePd() : null;
        this.time = a.getCourse() != null ? a.getCourse().getTraingTime() : null;
        this.total = a.getTotal();
        this.present = a.getPresent();
        this.late = a.getLate();
        this.absent = a.getAbsent();
        this.checkedOut = a.getCheckedOut();
        this.allCheckedOut = a.isAllCheckedOut();
        this.attendees = a.getRoster().stream().map(Attendee::new).toList();
    }

    /** 브라우저 표시용 최소 수강생 정보(주민번호 제외). */
    @Getter
    public static class Attendee {
        private final String name;
        private final String type;        // 실업자/재직자
        private final String status;      // 출결 상태명
        private final String checkInTime;  // 입실 HHmm
        private final String checkOutTime; // 퇴실 HHmm
        private final boolean checkedOut;

        Attendee(HrdAttendee a) {
            this.name = a.getCstmrNm();
            this.type = a.getTrneeSeNm();
            this.status = a.getAtendSttusNm();
            this.checkInTime = a.getCheckInTime();
            this.checkOutTime = a.getCheckOutTime();
            this.checkedOut = a.isCheckedOut();
        }
    }
}
