package com.park.welstory.wooriportal.saramin.enumlist.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사라민 연봉 최소 코드 (sal_min 파라미터)
 * "이 연봉 이상" 하한선으로 동작
 *
 * 예) sal_min=2 → 2,000만원 이상
 */
@Getter
@RequiredArgsConstructor
public enum SalaryRange {

    MIN_2000("2",  "2,000만원 이상"),
    MIN_2500("3",  "2,500만원 이상"),
    MIN_3000("4",  "3,000만원 이상"),
    MIN_3500("5",  "3,500만원 이상"),
    MIN_4000("6",  "4,000만원 이상"),
    MIN_4500("7",  "4,500만원 이상"),
    MIN_5000("8",  "5,000만원 이상"),
    MIN_6000("9",  "6,000만원 이상"),
    MIN_7000("10", "7,000만원 이상"),
    MIN_8000("11", "8,000만원 이상"),
    MIN_9000("12", "9,000만원 이상"),
    MIN_1_EOKWON("13", "1억원 이상");

    private final String code;
    private final String name;
}