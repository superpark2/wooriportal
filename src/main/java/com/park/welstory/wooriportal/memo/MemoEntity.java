package com.park.welstory.wooriportal.memo;

import com.park.welstory.wooriportal.common.baseentity.BaseEntity;
import com.park.welstory.wooriportal.member.MemberEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "memo")
public class MemoEntity extends BaseEntity {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memoNum;

    @Column(columnDefinition = "LONGTEXT")
    private String memoContent;
    private String divisionGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberNum")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private MemberEntity member;
}
