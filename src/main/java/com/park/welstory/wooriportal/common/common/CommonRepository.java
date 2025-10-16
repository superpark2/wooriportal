package com.park.welstory.wooriportal.common.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommonRepository extends JpaRepository<CommonEntity, Long> {

    Page<CommonEntity> findByCategory(String category, Pageable pageable);

}
