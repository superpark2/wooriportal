package com.park.welstory.wooriportal.common.memo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.park.welstory.wooriportal.member.MemberDTO;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MemoDTO {
    private Long memoNum;
    private String memoContent;
    private String divisionGroup;
    private LocalDateTime createdAt;
    

    private MemberDTO member;
}
