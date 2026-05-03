package com.park.welstory.wooriportal.saramin.dto;

import lombok.Data;
import java.util.List;

@Data
public class SearchRequestDto {

    // ── 위치 ─────────────────────────────────────────────────────────────────
    /** 시/군/구 단위 코드 (loc_cd). LocationCode.getCode() 값 */
    private String locCd;

    /** 시/도 단위 코드 (loc_mcd). LocationCode.getParentCode() 값 */
    private String locMcd;

    // ── 키워드 ────────────────────────────────────────────────────────────────
    /** 검색어 (searchword). URL 인코딩은 buildSearchUrl 내부에서 처리 */
    private String searchWord;

    /** 검색 유형 (searchType). 키워드 검색 시 "search" 고정 */
    private String searchType = "search";

    // ── 직종 ─────────────────────────────────────────────────────────────────
    /**
     * 직종 대분류 코드 (cat_mcls). 전체 선택 시 사용.
     *   예) "2" = IT·개발·데이터 전체
     * jobDetailCode 와 동시에 사용하지 않음 — 대분류 선택 시 이것만 세팅.
     */
    private List<String> jobMcls;

    /**
     * 직종 세부 코드 (cat_kewd). 세부 직무 선택 시 사용.
     *   예) Code2IT.BACKEND_DEV.getCode() = "84"
     * jobMcls 와 동시에 사용하지 않음 — 세부 선택 시 이것만 세팅.
     */
    private List<String> jobCode;

    // ── 고용형태 ──────────────────────────────────────────────────────────────
    /** 고용형태 코드 목록 (job_type). EmploymentType.getCode() 값 */
    private List<String> jobType;

    // ── 기업형태 ──────────────────────────────────────────────────────────────
    /** 기업규모 코드 목록 (company_type). scale001~scale006 */
    private List<String> companyType;

    // ── 경력 ─────────────────────────────────────────────────────────────────
    /**
     * 경력 구분 코드 목록 (exp_cd).
     *   1 = 신입, 2 = 경력, 3 = 신입·경력, 4 = 경력무관
     */
    private List<String> expCd;

    /** 경력 최소 년수 (exp_min). exp_cd=2 일 때 사용 */
    private String expMin;

    /** 경력 최대 년수 (exp_max). exp_cd=2 일 때 사용 */
    private String expMax;

    // ── 학력 ─────────────────────────────────────────────────────────────────
    /** 학력 상한 코드 (edu_max). EducationLevel.getCode() 값 */
    private String eduMax;

    // ── 급여 ─────────────────────────────────────────────────────────────────
    /** 연봉 최소 코드 (sal_min). SalaryRange.getCode() 값 */
    private String salMin;

    // ── 정렬 ─────────────────────────────────────────────────────────────────
    /**
     * 정렬 기준 (sort).
     *   RL = 관련도순(기본), DA = 최신등록순, RD = 마감일순, PD = 인기순
     */
    private String sort;

    // ── 페이징 ────────────────────────────────────────────────────────────────
    private int page      = 1;
    private int pageCount = 25;
}