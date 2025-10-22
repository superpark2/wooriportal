package com.park.welstory.wooriportal.pcinfo;

import com.park.welstory.wooriportal.location.LocationDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@RequiredArgsConstructor
public class PcInfoDTO {

    private Long pcInfoNum;
    private String pcInfoSeatNum;
    private String pcInfoCpu;
    private String pcInfoStorage;
    private String pcInfoRam;
    private String pcInfoVga;
    private String pcInfoMonitor;
    private String pcInfoIp;
    private LocationDTO location;
    private Long locationNum;
    private String locationName;
    private String locationImageMeta;

    private Long buildingNum;
    private String buildingName;

    private MultipartFile pcInfoImage;
    private MultipartFile pcInfoSpecImage;

    private String pcInfoImageMeta;
    private String pcInfoSpecImageMeta;

}
