package com.park.welstory.wooriportal.saramin.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultDto {
    private List<JobPostingDto> postings;
    private String crawledUrl;
    private int totalCount;
    private boolean success;
    private String errorMessage;
    private int currentPage;
    private int totalPages;
    private int pageCount;
}
