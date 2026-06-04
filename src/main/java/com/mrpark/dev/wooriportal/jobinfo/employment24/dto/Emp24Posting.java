package com.mrpark.dev.wooriportal.jobinfo.employment24.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 고용24 채용공고 1건 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Emp24Posting {
    private String title;
    private String company;
    private String location;
    private String career;
    private String education;
    private String employmentType;
    private String salary;
    private String deadline;
    private String registeredDate;
    private String link;
}
