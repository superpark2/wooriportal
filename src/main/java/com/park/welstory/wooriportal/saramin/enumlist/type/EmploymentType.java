package com.park.welstory.wooriportal.saramin.enumlist.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사라민 고용형태 코드 (job_type 파라미터)
 * 다중 선택 시 %2C 로 구분: job_type=1%2C2
 */
@Getter
@RequiredArgsConstructor
public enum EmploymentType {

    REGULAR("1",   "정규직"),
    CONTRACT("2",  "계약직"),
    INTERN("3",    "인턴직"),
    PART_TIME("4", "아르바이트"),
    DISPATCH("5",  "파견직"),
    FREELANCE("6", "프리랜서"),
    DAILY("7",     "일용직");

    private final String code;
    private final String name;
}