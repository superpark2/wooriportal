package com.park.welstory.wooriportal.common.file;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository <FileEntity, Long> {

    List<FileEntity> findByBoardNumAndDivisionGroup(Long boardNum, String divisionGroup);
    void deleteByBoardNumAndDivisionGroup(Long boardNum, String divisionGroup);

}
