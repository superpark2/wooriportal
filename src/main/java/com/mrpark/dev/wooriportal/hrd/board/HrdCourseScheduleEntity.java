package com.mrpark.dev.wooriportal.hrd.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 과정별 강의 요일 스케줄. (과정ID + 회차) 단위.
 *
 * <p>{@code daysOfWeek} = 강의 요일 CSV(1=월 … 7=일). 오늘 요일이 여기 없으면 비강의일로 보고
 * 학생 전원 '미출석'(결석 아님) 처리 + 전광판에서 자동 접힘.</p>
 */
@Entity
@Table(name = "hrd_course_schedule")
@Getter @Setter
public class HrdCourseScheduleEntity {

    /** 과정키 = tracseId + "|" + tracseTme */
    @Id
    @Column(name = "course_key", length = 60)
    private String courseKey;

    @Column(name = "tracse_id", length = 30)
    private String tracseId;

    @Column(name = "tracse_tme", length = 10)
    private String tracseTme;

    /** 강의 요일 CSV (예: "1,2,3,4,5" = 월~금). 빈값/없음 = 매일 강의로 간주. */
    @Column(name = "days_of_week", length = 20)
    private String daysOfWeek;

    public static String key(String tracseId, String tracseTme) {
        return tracseId + "|" + tracseTme;
    }
}
