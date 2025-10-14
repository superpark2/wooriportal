package com.park.welstory.wooriportal.member;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ToString
public class MemberDTO {

    private Long memberNum;
    private String memberId;
    private String memberName;
    private String memberPassword;
    private MultipartFile memberPicture;
    private String memberPictureMeta;
    private String memberComment;
    private String memberRole;
}
