package com.park.welstory.wooriportal.personal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonalRepository extends JpaRepository<PersonalEntity, Long> {

    Page<PersonalEntity> findByCategoryAndMember_MemberNum(String category, Long memberNum, Pageable pageable);
    List<PersonalEntity> findByCategory(String category);

}
