package com.park.welstory.wooriportal.log;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LogDTO {
    
    private Long logNum;
    private String logContent;
    private Long pcinfoNum;
    private LocalDateTime createdAt;

    // 최근 장비 변동내역에서 PC의 장소명, 좌석번호 표시용
    private String locationName;
    private String seatNum;
} 