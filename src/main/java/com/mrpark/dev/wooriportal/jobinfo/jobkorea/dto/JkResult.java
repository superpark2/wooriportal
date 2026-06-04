package com.mrpark.dev.wooriportal.jobinfo.jobkorea.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 잡코리아 검색 결과 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JkResult {
    private List<JkPosting> postings;
    private String crawledUrl;
    private int totalCount;
    private int currentPage;
    private int pageCount;
    private int totalPages;
    private boolean success;
    private String errorMessage;
}
