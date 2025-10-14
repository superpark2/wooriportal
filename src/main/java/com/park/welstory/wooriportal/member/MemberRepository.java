package com.park.welstory.wooriportal.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByMemberId(String memberId);
    MemberEntity findByMemberNum(Long memberNum);

}
