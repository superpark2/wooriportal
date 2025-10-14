package com.park.welstory.wooriportal.pcinfo.pcinfoRequire;

import com.park.welstory.wooriportal.pcinfo.PcInfoDTO;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
public class PcInfoRequireDTO {

    private Long reNum;
    private String reContent;
    private String reSeat;
    private String reType;
    private String reWriter;
    private PcInfoRequireDTO reParent;
    private String reStatus;

    private PcInfoDTO pcInfo;

    private LocalDateTime createdAt;
    
    private List<PcInfoRequireDTO> answers;

}
