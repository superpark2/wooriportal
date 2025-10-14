package com.park.welstory.wooriportal.common.board;

import com.park.welstory.wooriportal.common.baseentity.BaseEntity;
import com.park.welstory.wooriportal.member.MemberEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
public class BoardEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardNum;

    private String boardTitle;

    @Column(columnDefinition = "LONGTEXT")
    private String boardContent;

    private boolean isNotice;

    private String category;
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberNum")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private MemberEntity member;



}
