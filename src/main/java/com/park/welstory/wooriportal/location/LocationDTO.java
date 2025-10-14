package com.park.welstory.wooriportal.location;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ToString
public class LocationDTO {

    private Long locationNum;
    private String locationName;
    private String locationType;
    private String locationDescription;

    private LocationEntity locationParent;

    private MultipartFile locationImage;
    private String locationImageMeta;
}
