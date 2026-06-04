package com.mrpark.dev.wooriportal.jobinfo.jobkorea.dto;

import lombok.Data;

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
}
