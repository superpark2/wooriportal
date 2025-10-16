package com.park.welstory.wooriportal.todolist;

import com.park.welstory.wooriportal.common.baseentity.BaseEntity;
import com.park.welstory.wooriportal.member.MemberEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "todolist")
public class TodoListEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoNum;
    private String todoTitle;

    @Column(columnDefinition = "LONGTEXT")
    private String todoContent;

    private LocalDateTime doneDate;

    private Boolean todoDone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberNum")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MemberEntity member;
}
