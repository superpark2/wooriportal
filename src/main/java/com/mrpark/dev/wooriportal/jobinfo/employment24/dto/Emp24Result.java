package com.mrpark.dev.wooriportal.jobinfo.employment24.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 고용24 검색 결과 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Emp24Result {
    private List<Emp24Posting> postings;
    private String crawledUrl;
    private int totalCount;
    private int currentPage;
    private int pageCount;
    private int totalPages;
    private boolean success;
    private String errorMessage;
}
