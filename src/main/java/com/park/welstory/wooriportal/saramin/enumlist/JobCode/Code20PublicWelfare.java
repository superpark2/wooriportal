package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code20PublicWelfare {

    FAMILY_COUNSELING(1952, "가족상담"),
    CAMPAIGNER(1928, "캠페이너"),
    LIBRARIAN(1929, "도서관사서"),
    ELDERLY_WELFARE(1953, "노인복지"),
    PLAY_THERAPY(1954, "놀이치료"),
    LIFELONG_EDUCATOR(1930, "평생교육사"),
    LIBRARY_MANAGEMENT(1955, "도서관리"),
    CARE_TEACHER(1931, "돌봄교사"),
    ART_THERAPY(1956, "미술치료"),
    PROTECTION_COUNSELOR(1932, "보호상담원"),
    SECRETARY_GENERAL(1933, "사무국장"),
    AFTER_SCHOOL_ACADEMY(1957, "방과후아카데미"),
    PASTOR(1934, "목회자"),
    VISITING_BATH(1958, "방문목욕"),
    VISITING_CARE(1959, "방문요양"),
    OFFICE_WORK(1935, "사무직"),
    CASE_MANAGEMENT(1960, "사례관리"),
    SOCIAL_WORKER(1936, "사회복지사"),
    CHILD_CARE(1961, "아동보육"),
    LIVING_WELFARE_WORKER(1937, "생활복지사"),
    CHILD_WELFARE(1962, "아동복지"),
    LIVING_GUIDANCE_OFFICER(1938, "생활지도원"),
    LIVING_SUPPORT_WORKER(1939, "생활지원사"),
    MUSIC_THERAPY(1963, "음악치료"),
    PSYCHOTHERAPIST(1940, "심리치료사"),
    COGNITIVE_THERAPY(1964, "인지치료"),
    CARE_GIVER(1941, "요양보호사"),
    VOLUNTEER(1965, "자원봉사"),
    OCCUPATIONAL_THERAPY(1966, "작업치료"),
    FIXED_TERM_OFFICIAL(1942, "임기제공무원"),
    DISABILITY_WELFARE(1967, "장애인복지"),
    MILITARY_NCO(1943, "군인·부사관"),
    YOUTH_WELFARE(1968, "청소년복지"),
    MILITARY_SERVICE_EXCEPTION(1944, "병역특례"),
    HOSPICE(1969, "호스피스"),
    REHABILITATION_TEACHER(1945, "재활교사"),
    EAP_COUNSELING(1970, "EAP상담"),
    VOCATIONAL_COUNSELOR(1946, "직업상담사"),
    MARC_CONSTRUCTION(1971, "MARC구축"),
    YOUTH_INSTRUCTOR(1947, "청소년지도사"),
    SPECIAL_EDUCATION_TEACHER(1948, "특수교사"),
    ACTIVITY_SUPPORT_WORKER(1949, "활동지원사"),
    SENSORY_INTEGRATION_THERAPIST(1950, "감각통합치료사"),
    SPEECH_THERAPIST(1951, "언어치료사");

    private final int code;
    private final String name;

    Code20PublicWelfare(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code20PublicWelfare fromCode(int code) {
        for (Code20PublicWelfare c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}