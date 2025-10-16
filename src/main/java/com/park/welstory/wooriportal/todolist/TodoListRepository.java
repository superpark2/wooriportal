package com.park.welstory.wooriportal.todolist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoListRepository extends JpaRepository<TodoListEntity, Long> {

    Page<TodoListEntity> findByMember_MemberNum(Long memberNum, Pageable pageable);

}
