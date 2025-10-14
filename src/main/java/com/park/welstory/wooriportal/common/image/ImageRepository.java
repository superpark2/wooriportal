package com.park.welstory.wooriportal.common.image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository <ImageEntity, Long> {
    List<ImageEntity> findByBoardNumAndDivisionGroup(Long boardNum, String group);
    ImageEntity findByImagePath(String imagePath);

}
