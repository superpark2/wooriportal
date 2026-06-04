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

    /** 학생 연락처(있으면). HRD 응답에 따라 비어있을 수 있음. */
    private String telno;

    /** 규칙으로 재판정한 출결상태(출석/지각/결석/미출석/중도탈락). */
    private String computedStatus;
    /** 퇴실 판정(퇴실/조퇴/미퇴실/null). */
    private String checkoutStatus;

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
        a.setTelno(parsePhone(ds, row));
        return a;
    }

    /** 연락처 best-effort 추출: 전화 컬럼 후보 → pinfo 블록 내 전화 패턴. 없으면 null. */
    private static String parsePhone(SsvDataset ds, int row) {
        for (String col : new String[]{"telno", "mbtlnum", "cralTelno", "cryalTelno", "moblphonNo", "hpNo"}) {
            if (ds.hasColumn(col)) {
                String v = ds.getString(row, col);
                if (v != null && v.matches(".*\\d{2,3}-?\\d{3,4}-?\\d{4}.*")) {
                    return v.trim();
                }
            }
        }
        if (ds.hasColumn("pinfo")) {
            String pinfo = ds.getString(row, "pinfo");
            if (pinfo != null) {
                java.util.regex.Matcher m =
                        java.util.regex.Pattern.compile("01[016789][- ]?\\d{3,4}[- ]?\\d{4}").matcher(pinfo);
                if (m.find()) {
                    return m.group();
                }
            }
        }
        return null;
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
