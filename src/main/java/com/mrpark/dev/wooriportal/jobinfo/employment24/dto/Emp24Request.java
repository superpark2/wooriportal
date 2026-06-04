package com.mrpark.dev.wooriportal.jobinfo.employment24.dto;

import lombok.Data;

import java.util.List;

/**
 * 고용24(워크넷) 채용정보 검색 요청.
 * work24 list 엔드포인트는 페이지당 10건 고정이며 pageIndex 로 페이징한다.
 * 파라미터명은 work24 hidden submit 필드 기준(실측 검증 완료).
 */
@Data
public class Emp24Request {

    /** 검색어 (keyword) */
    private String keyword;

    /** 지역 시도 코드 (region). 5자리. 예) 서울=11000, 경기=41000. 빈값/00000=전국 */
    private String region;

    /** 경력 (careerTypes). N=신입, E=경력, Z=관계없음 */
    private List<String> careerTypes;

    /** 학력 (academicGbn). 01,02=중졸이하, 03=고졸, 04=대졸(2~3년), 05=대졸(4년), 06=석사, 07=박사, 00=학력무관 */
    private List<String> academicGbn;

    /** 임금 구분 (payGbn). H=시급, D=일급, M=월급, Y=연봉 */
    private String payGbn;

    /** 최소 임금 (minPay). 연봉/월급은 만원 단위 */
    private String minPay;

    /** 최대 임금 (maxPay) */
    private String maxPay;

    /** 상용/일용 (empTpGbcd). 1=상용직, 2=일용직 */
    private List<String> empTpGbcd;

    /** 근무형태 (holidayGbn). 1=주5일, 2=주6일, 4=주4일, 5=주3일, 6=주2일, 7=주1일 */
    private List<String> holidayGbn;

    /** 기업형태 (enterPriseGbn). 01=대기업, 04=공무원/공기업/공공기관, 05=외국계, 06=코스피, 07=코스닥, 20=중견, 09/10/11=인증기업 */
    private List<String> enterPriseGbn;

    /** 고용형태 (employGbn). 10/11=기간정함없음, 20/21=기간정함있음, 4=파견근로 */
    private List<String> employGbn;

    /** 직종 코드 (occupation). work24 직업분류 코드. 대분류 2자리/중분류 3자리 등. 다중 콤마 */
    private List<String> occupation;

    /** 페이지 번호. 1부터 시작 */
    private int page = 1;

    /** 한 페이지에 볼 건수(25/50/100). work24는 10건 고정이라 내부 페이지를 모아 슬라이스한다. */
    private int pageCount = 25;
}
