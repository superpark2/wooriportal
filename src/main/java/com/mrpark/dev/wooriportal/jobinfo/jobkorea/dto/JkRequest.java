package com.mrpark.dev.wooriportal.jobinfo.jobkorea.dto;

import lombok.Data;
import java.util.List;

/**
 * 잡코리아 검색 요청.
 * 잡코리아 검색은 페이지당 20건 고정이며 Page_No 로 페이징한다.
 */
@Data
public class JkRequest {

    /** 검색어 (stext) */
    private String keyword;

    /** 페이지 번호. 1부터 시작 */
    private int page = 1;

    /** 한 페이지에 볼 건수(25/50/100). 잡코리아는 20건 고정이라 내부 페이지를 모아 슬라이스한다. */
    private int pageCount = 20;

    /** 지역 코드 목록 (예: 101000=서울, 102000=경기) */
    private List<String> location;

    /** 경력 유형: 1=신입, 2=경력, 3=신입·경력 */
    private String careerType;

    /** 고용형태 코드 목록 (예: 1=정규직, 2=계약직, …) */
    private List<String> empType;

    /** 학력: 0=무관, 1=고졸, 2=초대졸, 3=대졸, 4=석사, 5=박사 */
    private String eduLevel;
}
