package com.park.welstory.wooriportal.common.file;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class FileService {
    private final FileRepository fileRepository;

    @Transactional
    public void addFile(String group, String category, Long boardNum, List<MultipartFile> files) {

        if (files == null || files.isEmpty()) return;

        List<FileEntity> existingFiles = fileRepository.findByBoardNumAndDivisionGroup(boardNum, group);

        List<String> newFileNames = new ArrayList<>();
        for (MultipartFile file : files) {
            newFileNames.add(file.getOriginalFilename());
        }

        for (FileEntity existing : existingFiles) {
            if (!newFileNames.contains(existing.getFileName())) {
                File f = new File(existing.getFilePath());
                if (f.exists()) f.delete();

                fileRepository.delete(existing);
            }
        }

        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        String basePath = System.getProperty("user.dir");
        String LocalPath = basePath + "/file/" + group + "/" + category + "/file/" + date;
        String filePath = "/file/" + group + "/" + category + "/file/" + date;
        File uploadDir = new File(LocalPath);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();

            boolean alreadyExists = false;
            for (FileEntity f : existingFiles) {
                if (f.getFileName().equals(originalName)) {
                    alreadyExists = true;
                    break;
                }
            }

            if (!alreadyExists && !file.isEmpty()) {
                String newFilename = UUID.randomUUID().toString().substring(0, 8) + "_" + originalName;
                File saveFile = new File(uploadDir, newFilename);

                try {
                    file.transferTo(saveFile);

                    FileEntity entity = new FileEntity();
                    entity.setFileName(newFilename);
                    entity.setFileSize(file.getSize());
                    entity.setFilePath(filePath + "/" + newFilename);
                    entity.setDivisionGroup(group);
                    entity.setDivisionCategory(category);
                    entity.setBoardNum(boardNum);

                    fileRepository.save(entity);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Transactional
    public void deleteFile(String group, Long num) {
        List<FileEntity> files = fileRepository.findByBoardNumAndDivisionGroup(num, group);

        String basePath = System.getProperty("user.dir");

        for (FileEntity file : files) {
            File localFile = new File(basePath, file.getFilePath());
            if (localFile.exists()) {
                localFile.delete();
            }
        }
        fileRepository.deleteByBoardNumAndDivisionGroup(num, group);
    }
}