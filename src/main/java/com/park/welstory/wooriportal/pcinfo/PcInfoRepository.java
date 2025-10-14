package com.park.welstory.wooriportal.pcinfo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PcInfoRepository extends JpaRepository<PcInfoEntity,Long> {
    List<PcInfoEntity> findByLocation_LocationNum(Long location);

}
