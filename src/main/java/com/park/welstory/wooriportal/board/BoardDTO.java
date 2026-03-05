package com.park.welstory.wooriportal.board;

import com.park.welstory.wooriportal.global.file.FileEntity;
import com.park.welstory.wooriportal.member.MemberDTO;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

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

    /** 새로 업로드할 파일 목록 */
    private List<MultipartFile> newFiles;

    /** 유지할 기존 파일 경로 목록 */
    private List<String> existingFilePaths;

    /** 상세 조회 시 파일 메타 정보 */
    private List<FileEntity> filesMeta;

    private boolean isWriter;
}
