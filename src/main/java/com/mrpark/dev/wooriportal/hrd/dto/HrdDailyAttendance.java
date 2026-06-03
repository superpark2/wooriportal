package com.mrpark.dev.wooriportal.hrd.dto;

import java.util.List;
import lombok.Getter;

/**
 * 한 과정의 당일 출결 원자료(과정 헤더 + 수강생 목록). 집계/판정은 {@code HrdBoardRow} 에서 한다.
 */
@Getter
public class HrdDailyAttendance {

    private final HrdCourseDetail course;
    private final List<HrdAttendee> roster;

    public HrdDailyAttendance(HrdCourseDetail course, List<HrdAttendee> roster) {
        this.course = course;
        this.roster = roster;
    }
}
