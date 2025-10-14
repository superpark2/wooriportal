package com.park.welstory.wooriportal.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<LocationEntity,Long> {

    List<LocationEntity> findByLocationType(String type);

    List<LocationEntity> findByLocationTypeAndLocationParent_LocationNum(String type, Long buildingNum);

}
