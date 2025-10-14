package com.park.welstory.wooriportal.common.image;

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
@Table(name="image")
public class ImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long imageNum;
    private String imageName;
    private String imagePath;
    private String imageType;
    private Long imageSize;

    private String divisionGroup;
    private String divisionCategory;
    private Long boardNum;
}