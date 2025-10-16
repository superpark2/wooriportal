package com.park.welstory.wooriportal.memo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoRepository extends JpaRepository<MemoEntity, Long> {

//    @Query("SELECT m FROM MemoEntity m LEFT JOIN FETCH m.member ORDER BY m.memoNum DESC")
//    Page<MemoEntity> findAllMemos(Pageable pageable);

    Page<MemoEntity> findByDivisionGroup(String group, Pageable pageable);
    Page<MemoEntity> findByDivisionGroupAndMember_MemberNum(String group, Long memberNum, Pageable pageable);


}