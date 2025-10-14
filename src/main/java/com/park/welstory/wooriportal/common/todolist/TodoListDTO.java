package com.park.welstory.wooriportal.common.todolist;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.park.welstory.wooriportal.member.MemberDTO;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TodoListDTO {

    private Long todoNum;
    private String todoTitle;
    private String todoContent;


    private LocalDateTime doneDate;

    private Boolean todoDone;

    private LocalDateTime createdAt;

    private MemberDTO member;

}
