package com.mrpark.dev.wooriportal.jobinfo.jobkorea.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 잡코리아 채용공고 1건 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JkPosting {
    private String title;
    private String company;
    private String location;
    /** 업종 · 직무 키워드 (예: "신문·잡지·언론사, 백엔드개발자, 프론트엔드개발자") */
    private String jobCategory;
    /** 경력 정보 (예: "신입·경력3년↑") */
    private String experience;
    private String badge;
    private String deadline;
    private String link;
}
