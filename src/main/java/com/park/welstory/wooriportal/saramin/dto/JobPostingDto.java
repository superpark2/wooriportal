package com.park.welstory.wooriportal.saramin.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobPostingDto {
    private String title;
    private String company;
    private String location;
    private String experience;
    private String education;
    private String salary;
    private String deadline;
    private String employmentType;
    private String link;
    private String badge;
}
