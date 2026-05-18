package com.mrpark.dev.wooriportal.memo;

import com.mrpark.dev.wooriportal.member.MemberDTO;
import lombok.*;

import java.time.LocalDateTime;

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
