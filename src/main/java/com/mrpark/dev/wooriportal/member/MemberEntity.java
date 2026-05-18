package com.mrpark.dev.wooriportal.member;

import com.mrpark.dev.wooriportal.global.base.BaseEntity;
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
    private String memberId;

    @Column(nullable = false)
    private String memberName;

    @Column(nullable = false)
    private String memberPassword;

    private String memberPictureMeta;
    private String memberComment;
    private String memberRole;
}
