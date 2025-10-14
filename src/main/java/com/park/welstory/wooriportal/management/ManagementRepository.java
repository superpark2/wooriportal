package com.park.welstory.wooriportal.management;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagementRepository extends JpaRepository<ManagementEntity, Long> {

    Page<ManagementEntity> findByCategory(String category, Pageable pageable);
    List<ManagementEntity> findByCategory(String category);

}
