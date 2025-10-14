package com.park.welstory.wooriportal.common.board;

import com.park.welstory.wooriportal.common.file.FileEntity;
import com.park.welstory.wooriportal.member.MemberDTO;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Getter
@Setter
@ToString
@Data
public class BoardDTO {

    private Long boardNum;
    private String boardTitle;
    private String boardContent;
    private boolean isNotice;

    private MemberDTO member = new MemberDTO();

    private String group;
    private String category;
    private String type;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<MultipartFile> files;
    private List<FileEntity> filesMeta;

    private boolean isWriter;
}
