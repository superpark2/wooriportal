package com.park.welstory.wooriportal.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LogRepository extends JpaRepository<LogEntity, Long> {

    // 최근 로그 조회
    @Query("SELECT l FROM LogEntity l ORDER BY l.createdAt DESC")
    Page<LogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 모든 로그 조회
    Page<LogEntity> findAll(Pageable pageable);
} 