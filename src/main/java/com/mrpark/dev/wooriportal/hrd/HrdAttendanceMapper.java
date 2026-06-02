package com.mrpark.dev.wooriportal.hrd;

import com.mrpark.dev.wooriportal.hrd.dto.HrdAttendee;
import com.mrpark.dev.wooriportal.hrd.dto.HrdCourse;
import com.mrpark.dev.wooriportal.hrd.dto.HrdCourseDetail;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvData;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvDataset;
import java.util.ArrayList;
import java.util.List;

/**
 * 디코딩된 HRD-Net SSV 응답({@link SsvData})을 도메인 DTO로 매핑한다.
 */
public final class HrdAttendanceMapper {

    private HrdAttendanceMapper() {
    }

    /** {@code selectAtendList.do} → 당일 과정 목록. */
    public static List<HrdCourse> toCourses(SsvData data) {
        SsvDataset ds = data.getDataset("ds_atendInfoList");
        List<HrdCourse> courses = new ArrayList<>();
        if (ds == null) {
            return courses;
        }
        for (int i = 0; i < ds.getRowCount(); i++) {
            courses.add(HrdCourse.from(ds, i));
        }
        return courses;
    }

    /** {@code selectDailAtndceDetail.do} → 과정 헤더. 행이 없으면 {@code null}. */
    public static HrdCourseDetail toCourseDetail(SsvData data) {
        SsvDataset ds = data.getDataset("ds_dailAtdbTraing");
        if (ds == null || ds.getRowCount() == 0) {
            return null;
        }
        return HrdCourseDetail.from(ds, 0);
    }

    /** {@code selectDailAtndceDetail.do} → 수강생별 출결 목록. */
    public static List<HrdAttendee> toRoster(SsvData data) {
        SsvDataset ds = data.getDataset("ds_dailAtendList");
        List<HrdAttendee> roster = new ArrayList<>();
        if (ds == null) {
            return roster;
        }
        for (int i = 0; i < ds.getRowCount(); i++) {
            roster.add(HrdAttendee.from(ds, i));
        }
        return roster;
    }
}
