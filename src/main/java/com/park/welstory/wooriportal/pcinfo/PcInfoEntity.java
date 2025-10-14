package com.park.welstory.wooriportal.pcinfo;

import com.park.welstory.wooriportal.location.LocationEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table(name="pcinfo")
public class PcInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pcInfoNum;
    private String pcInfoSeatNum;
    private String pcInfoCpu;
    private String pcInfoStorage;
    private String pcInfoRam;
    private String pcInfoVga;
    private String pcInfoMonitor;
    private String pcInfoIp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private LocationEntity location;

    private String pcInfoImageMeta;
    private String pcInfoSpecImageMeta;
}