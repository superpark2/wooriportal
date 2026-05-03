package com.park.welstory.wooriportal.saramin.enumlist.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사라민 학력 코드 (edu_max 파라미터)
 * "이 학력 이하" 상한선으로 동작
 *
 * 예) edu_max=6 → 대학교(4년) 이하 모두 포함
 *     edu_max=9 → 학력 무관 (전체)
 */
@Getter
@RequiredArgsConstructor
public enum EducationLevel {

    MIDDLE_SCHOOL("1",      "중졸이하"),
    HIGH_SCHOOL("2",        "고졸"),
    COLLEGE_2Y("3",         "대학(2,3년)졸"),
    COLLEGE_4Y("6",         "대학교(4년)졸"),
    GRADUATE("7",           "대학원졸"),
    NO_RESTRICTION("9",     "학력무관");

    private final String code;
    private final String name;
}