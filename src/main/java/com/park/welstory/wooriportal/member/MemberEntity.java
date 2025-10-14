package com.park.welstory.wooriportal.member;

import com.park.welstory.wooriportal.common.baseentity.BaseEntity;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import jakarta.persistence.*;
import lombok.*;

@RequiredArgsConstructor
@Entity
@Getter
@Setter
@ToString
@Table(name = "member")
public class MemberEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberNum;

    @Column(unique = true, nullable = false)
    @NotNull
    private String memberId;

    @NotNull
    private String memberName;

    @NotNull
    private String memberPassword;

    private String memberPictureMeta;
    private String memberComment;
    private String memberRole;
}