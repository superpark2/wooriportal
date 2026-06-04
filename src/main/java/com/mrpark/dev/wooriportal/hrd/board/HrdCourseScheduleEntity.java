package com.mrpark.dev.wooriportal.hrd.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 과정별 강의요일 + 특이사항(전체 공유). (과정ID + 회차) 단위.
 *
 * <p>{@code daysOfWeek} = 강의요일 CSV(1=월…7=일). 오늘 요일이 없으면 비강의일 → 전원 미출석 + 자동 접힘.
 * {@code notes} = 특이사항(공유 메모). 끝난 과정(종료일+7일 경과)은 스케줄러가 자동 삭제.</p>
 */
@Entity
@Table(name = "hrd_course_schedule")
@Getter @Setter
public class HrdCourseScheduleEntity {

    @Id
    @Column(name = "course_key", length = 60)
    private String courseKey;

    @Column(name = "tracse_id", length = 30)
    private String tracseId;

    @Column(name = "tracse_tme", length = 10)
    private String tracseTme;

    /** 강의요일 CSV (예: "1,2,3,4,5"). 빈값 = 매일. */
    @Column(name = "days_of_week", length = 20)
    private String daysOfWeek;

    /** 특이사항(공유 메모). */
    @Column(name = "notes", length = 1000)
    private String notes;

    /** 과정 종료일 yyyyMMdd (자동삭제 기준). */
    @Column(name = "tracse_end_de", length = 8)
    private String tracseEndDe;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    public static String key(String tracseId, String tracseTme) {
        return tracseId + "|" + tracseTme;
    }
}
