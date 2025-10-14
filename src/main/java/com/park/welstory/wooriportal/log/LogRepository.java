package com.park.welstory.wooriportal.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogRepository extends JpaRepository<LogEntity, Long> {
    
    @Query("SELECT q FROM LogEntity q WHERE q.pcinfoNum = :pcinfoNum ORDER BY q.createdAt DESC")
    Page<LogEntity> findByPcinfoNum(@Param("pcinfoNum") Long pcinfoNum, Pageable pageable);

    @Query("SELECT q FROM LogEntity q ORDER BY q.createdAt DESC")
    Page<LogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
} 