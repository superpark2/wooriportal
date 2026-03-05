package com.park.welstory.wooriportal.global.file;

import com.park.welstory.wooriportal.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
@Entity
@Getter
@Setter
@ToString
@Table(name="file")
public class FileEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long fileNum;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;

    private String divisionGroup;
    private String divisionCategory;
    private Long boardNum;
}
