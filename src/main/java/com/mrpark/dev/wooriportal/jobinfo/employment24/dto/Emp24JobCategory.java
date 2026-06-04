package com.mrpark.dev.wooriportal.jobinfo.employment24.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 고용24 직종(직업분류) 노드 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Emp24JobCategory {
    /** 직종 코드 (occupation 파라미터 값) */
    private String code;
    /** 직종명 */
    private String name;
    /** 하위 노드 존재 가능 여부 (대분류/중분류는 true, 말단은 false 가정) */
    private boolean hasChild;
}
