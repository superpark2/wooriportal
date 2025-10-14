package com.park.welstory.wooriportal.pcinfo.pcinfoRequire;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PcInfoRequireRepository extends JpaRepository<PcInfoRequireEntity, Long> {

    Page<PcInfoRequireEntity> findByPcInfo_PcInfoNumOrderByReNumDesc(Long pcInfoNum, Pageable pageable);

    Page<PcInfoRequireEntity> findAllByOrderByReNumDesc(Pageable pageable);

    Page<PcInfoRequireEntity> findByReTypeOrderByReNumDesc(String reType, Pageable pageable);

    java.util.List<PcInfoRequireEntity> findFirst1ByReParent_ReNumOrderByReNumDesc(Long reNum);

    java.util.List<PcInfoRequireEntity> findByReParent_ReNumOrderByReNumDesc(Long reNum);

    Page<PcInfoRequireEntity> findByReStatusOrderByReNumDesc(String reStatus, Pageable pageable);
}


