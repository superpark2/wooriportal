package com.park.welstory.wooriportal.board.facility;

import com.park.welstory.wooriportal.board.board.BoardEntity;
import com.park.welstory.wooriportal.member.MemberEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Setter
@Table(name="facility")
public class FacilityEntity extends BoardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long BoardNum;

    private String BoardTitle;

    @Column(columnDefinition = "LONGTEXT")
    private String BoardContent;

    private boolean isNotice = false;

    private String category;
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberNum")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private MemberEntity member;



}
