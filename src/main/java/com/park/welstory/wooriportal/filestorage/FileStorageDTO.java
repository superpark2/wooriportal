package com.park.welstory.wooriportal.filestorage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FileStorageDTO {

    private String name;
    private Long size;
    private String path;
    private boolean directory;
    private LocalDateTime lastModified;
}
