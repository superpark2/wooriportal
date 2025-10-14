package com.park.welstory.wooriportal.pcinfo.pcinfoRequire;

import com.park.welstory.wooriportal.common.baseentity.BaseEntity;
import com.park.welstory.wooriportal.pcinfo.PcInfoEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name="pcInfoRequire")
public class PcInfoRequireEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reNum;
    private String reContent;
    private String reSeat;
    private String reType;
    private String reWriter;
    private String reStatus;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name="reParent")
    private PcInfoRequireEntity reParent;

    @ManyToOne
    @JoinColumn(name="pcInfoNum")
    private PcInfoEntity pcInfo;

}
