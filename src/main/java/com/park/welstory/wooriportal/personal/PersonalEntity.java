package com.park.welstory.wooriportal.personal;

import com.park.welstory.wooriportal.common.board.BoardEntity;
import com.park.welstory.wooriportal.common.file.FileEntity;
import com.park.welstory.wooriportal.member.MemberEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name="personal")
public class PersonalEntity extends BoardEntity {

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
