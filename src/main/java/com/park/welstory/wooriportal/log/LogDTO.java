package com.park.welstory.wooriportal.log;

import com.park.welstory.wooriportal.pcinfo.PcInfoDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LogDTO {
    
    private Long logNum;
    private String logContent;
    private LocalDateTime createdAt;
    private PcInfoDTO pcInfo;
    private Long pcinfoNum;
    
    // PC 정보 관련 필드들
    private String buildingName;
    private String roomName;
    private String seatNum;
} 