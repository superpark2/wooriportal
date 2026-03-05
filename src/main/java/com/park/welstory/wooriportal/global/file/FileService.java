package com.park.welstory.wooriportal.global.file;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FileService {

    private final FileRepository fileRepository;

    private String basePath() {
        return System.getProperty("user.dir");
    }

    /**
     * 저장 경로: /file/{category}/{originalName}
     * 중복 시:  /file/{category}/{name}_2.ext, _3.ext ...
     */
    private String resolveFileName(File dir, String originalName) {
        File candidate = new File(dir, originalName);
        if (!candidate.exists()) return originalName;

        // name + ext 분리
        int dot = originalName.lastIndexOf('.');
        String base = dot >= 0 ? originalName.substring(0, dot) : originalName;
        String ext  = dot >= 0 ? originalName.substring(dot) : "";

        int seq = 2;
        while (true) {
            String name = base + "_" + seq + ext;
            if (!new File(dir, name).exists()) return name;
            seq++;
        }
    }

    @Transactional
    public void addFile(String group, String category, Long boardNum,
                        List<MultipartFile> newFiles,
                        List<String> existingFilePaths) {

        String base = basePath();

        // 유지 목록에 없는 기존 파일 삭제
        List<FileEntity> dbFiles = fileRepository.findByBoardNumAndDivisionGroup(boardNum, group);
        for (FileEntity dbFile : dbFiles) {
            boolean keep = existingFilePaths != null && existingFilePaths.contains(dbFile.getFilePath());
            if (!keep) {
                new File(base, dbFile.getFilePath()).delete();
                fileRepository.delete(dbFile);
            }
        }

        if (newFiles == null || newFiles.isEmpty()) return;

        // 저장 디렉토리: {workDir}/file/{category}/
        File uploadDir = new File(base + "/file/" + category);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        for (MultipartFile file : newFiles) {
            if (file == null || file.isEmpty()) continue;

            String originalName = file.getOriginalFilename();
            String savedName    = resolveFileName(uploadDir, originalName);
            String filePath     = "/file/" + category + "/" + savedName;

            try {
                file.transferTo(new File(uploadDir, savedName));

                FileEntity entity = new FileEntity();
                entity.setFileName(savedName);
                entity.setFileSize(file.getSize());
                entity.setFilePath(filePath);
                entity.setFileType(file.getContentType());
                entity.setDivisionGroup(group);
                entity.setDivisionCategory(category);
                entity.setBoardNum(boardNum);
                fileRepository.save(entity);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Transactional
    public void deleteFile(String group, Long boardNum) {
        String base = basePath();
        fileRepository.findByBoardNumAndDivisionGroup(boardNum, group)
                .forEach(f -> new File(base, f.getFilePath()).delete());
        fileRepository.deleteByBoardNumAndDivisionGroup(boardNum, group);
    }
}
