package com.mrpark.dev.wooriportal.hrd.ssv;

/**
 * SSV 데이터셋 컬럼 정의.
 *
 * @param name 컬럼명
 * @param type 컬럼 타입코드 (1 = 문자열, 4 = 숫자). 셀 인코딩 타입을 결정한다.
 * @param size 컬럼 크기(원본 보존용)
 * @param flag 컬럼 플래그(원본 보존용)
 */
public record SsvColumn(String name, int type, int size, int flag) {
}
