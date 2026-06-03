package com.mrpark.dev.wooriportal.hrd.dto;

import com.mrpark.dev.wooriportal.hrd.ssv.SsvDataset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HRD-Net {@code selectAtendList.do} 응답({@code ds_atendInfoList})의 한 과정.
 * 당일 전광판에 띄울 "오늘 열리는 과정" 한 건.
 */
@Getter @Setter @NoArgsConstructor
public class HrdCourse {

    /** 과정 ID (상세조회 키) */
    private String tracseId;
    /** 회차(기수) (상세조회 키) */
    private String tracseTme;
    /** 과정-훈련 구분코드 (상세조회 키, 예: C0061) */
    private String crseTracseSe;

    /** 과정명 */
    private String tracseNm;
    /** 과정 유형명 (예: 실업자(국기), 근로자, K-디지털트레이닝) */
    private String mainTracseNm;
    /** 관할 고용센터명 (예: 부천고용센터) */
    private String oprtnInsttNm;

    /** 과정 시작일 yyyyMMdd */
    private String tracseBeginDe;
    /** 과정 종료일 yyyyMMdd */
    private String tracseEndDe;

    /** 총 훈련생 수 */
    private Integer totTrneeCo;
    /** 운영 훈련생 수 */
    private Integer totOprtnTrneeCo;

    /** 운영 상태명 (예: 확정자신고확인) */
    private String traortnSttusNm;

    /** QR 출결 키 문자열: TRACSE_ID=...,TRACSE_TME=...,CRSE_TRACSE_SE=... */
    private String qrCharstCn;

    /** 목록 표시 순번 (no) */
    private Integer displayNo;

    public static HrdCourse from(SsvDataset ds, int row) {
        HrdCourse c = new HrdCourse();
        c.setTracseId(ds.getString(row, "tracseId"));
        c.setTracseTme(ds.getString(row, "tracseTme"));
        c.setCrseTracseSe(ds.getString(row, "crseTracseSe"));
        c.setTracseNm(ds.getString(row, "tracseNm"));
        c.setMainTracseNm(ds.getString(row, "mainTracseNm"));
        c.setOprtnInsttNm(ds.getString(row, "oprtnInsttNm"));
        c.setTracseBeginDe(ds.getString(row, "tracseBeginDe"));
        c.setTracseEndDe(ds.getString(row, "tracseEndDe"));
        c.setTotTrneeCo(HrdValues.toInt(ds.getString(row, "totTrneeCo")));
        c.setTotOprtnTrneeCo(HrdValues.toInt(ds.getString(row, "totOprtnTrneeCo")));
        c.setTraortnSttusNm(ds.getString(row, "traortnSttusNm"));
        c.setQrCharstCn(ds.getString(row, "qrCharstCn"));
        c.setDisplayNo(HrdValues.toInt(ds.getString(row, "no")));
        return c;
    }

    /** yyyy-MM-dd 표시용 시작일. */
    public String getStartDateDisplay() {
        return HrdValues.formatDate(tracseBeginDe);
    }

    /** yyyy-MM-dd 표시용 종료일. */
    public String getEndDateDisplay() {
        return HrdValues.formatDate(tracseEndDe);
    }
}
