package com.mrpark.dev.wooriportal.hrd.dto;

import com.mrpark.dev.wooriportal.hrd.ssv.SsvDataset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HRD-Net {@code selectDailAtndceDetail.do} 응답({@code ds_dailAtendList})의 수강생 1명 출결.
 */
@Getter @Setter @NoArgsConstructor
public class HrdAttendee {

    /** 수강생 이름 */
    private String cstmrNm;
    /** 훈련생 구분명 (예: 실업자, 재직자) */
    private String trneeSeNm;
    /** 마스킹 주민번호 (예: 800816-2******) — 동명이인 구분용 */
    private String maskEncptIhidnm;

    /** 출결 상태코드 */
    private String atendSttusCd;
    /** 출결 상태명 (예: 출석, 결석, 지각, 조퇴) */
    private String atendSttusNm;
    /** 훈련생 상태명 (예: 훈련중) */
    private String trneeSttusNm;

    /** 출결 일자 yyyyMMdd */
    private String atendDe;
    /** 입실 시각 HHmm (HRD: lpsilTime) */
    private String checkInTime;
    /** 퇴실 시각 HHmm (HRD: levromTime) */
    private String checkOutTime;

    public static HrdAttendee from(SsvDataset ds, int row) {
        HrdAttendee a = new HrdAttendee();
        a.setCstmrNm(ds.getString(row, "cstmrNm"));
        a.setTrneeSeNm(ds.getString(row, "trneeSeNm"));
        a.setMaskEncptIhidnm(ds.getString(row, "maskEncptIhidnm"));
        a.setAtendSttusCd(ds.getString(row, "atendSttusCd"));
        a.setAtendSttusNm(ds.getString(row, "atendSttusNm"));
        a.setTrneeSttusNm(ds.getString(row, "trneeSttusNm"));
        a.setAtendDe(ds.getString(row, "atendDe"));
        a.setCheckInTime(ds.getString(row, "lpsilTime"));
        a.setCheckOutTime(ds.getString(row, "levromTime"));
        return a;
    }

    /** 입실했는지(입실 시각 존재). */
    public boolean isCheckedIn() {
        return checkInTime != null && !checkInTime.isBlank();
    }

    /** 퇴실했는지(퇴실 시각 존재). */
    public boolean isCheckedOut() {
        return checkOutTime != null && !checkOutTime.isBlank();
    }
}
