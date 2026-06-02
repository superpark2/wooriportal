package com.mrpark.dev.wooriportal.hrd.dto;

import com.mrpark.dev.wooriportal.hrd.ssv.SsvDataset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HRD-Net {@code selectDailAtndceDetail.do} 응답({@code ds_dailAtdbTraing})의 과정 헤더 정보.
 */
@Getter @Setter @NoArgsConstructor
public class HrdCourseDetail {

    private String tracseId;
    private String tracseTme;
    /** 과정명 */
    private String tracseNm;
    /** 과정 기간 표시 문자열 (예: 2026-04-02~2026-06-05 (17회차)) */
    private String tracsePd;
    /** 당일 훈련일자 yyyyMMdd */
    private String traingDe;
    /** 당일 훈련시간 표시 (예: 19:00~22:00(총 3시간)) */
    private String traingTime;
    /** 당일 시작시각 HHmm */
    private String traingBeginTime;
    /** 당일 종료시각 HHmm */
    private String traingEndTime;

    public static HrdCourseDetail from(SsvDataset ds, int row) {
        HrdCourseDetail d = new HrdCourseDetail();
        d.setTracseId(ds.getString(row, "tracseId"));
        d.setTracseTme(ds.getString(row, "tracseTme"));
        d.setTracseNm(ds.getString(row, "tracseNm1"));
        d.setTracsePd(ds.getString(row, "tracsePd"));
        d.setTraingDe(ds.getString(row, "traingDe"));
        d.setTraingTime(ds.getString(row, "traingTime"));
        d.setTraingBeginTime(ds.getString(row, "traingBeginTime"));
        d.setTraingEndTime(ds.getString(row, "traingEndTime"));
        return d;
    }
}
