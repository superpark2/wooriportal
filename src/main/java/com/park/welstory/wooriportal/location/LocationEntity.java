package com.park.welstory.wooriportal.location;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="location")
public class LocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long locationNum;
    private String locationName;
    private String locationType;
    private String locationDescription;

    @ManyToOne
    @JoinColumn(name="locationParent")
    private LocationEntity locationParent;

    private String locationImageMeta;
}
