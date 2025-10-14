package com.park.welstory.wooriportal.common.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ImageService {


    private final ImageRepository imageRepository;

    //작성중 임시 이미지

    public Map<String, Object> saveTempImage(MultipartFile image, String group, String category) {
        String imageName = UUID.randomUUID().toString().substring(0, 8) + "_" + image.getOriginalFilename();
        String uploadPath = new File("file/" + group + "/" + category + "/image/temp").getAbsolutePath();
        File folder = new File(uploadPath);
        if (!folder.exists()) folder.mkdirs();
        File dest = new File(folder, imageName);

        try {
            image.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패: saveTempImage 메서드 " + image.getOriginalFilename(), e);
        }

        String url = "/file/" + group + "/" + category + "/image/temp/" + imageName;

        return Map.of(
                "result", List.of(Map.of(
                        "url", url,
                        "name", image.getOriginalFilename()
                ))
        );
    }

    //작성 완료. 이미지 영구저장.
    public String saveImage(Long num, String content, String group, String category, String writeDate) {

        //일단 안쓰이는 이미지 삭제
        List<ImageEntity> existingImages = imageRepository.findByBoardNumAndDivisionGroup(num, group);
        for (ImageEntity img : existingImages) {
            if (!content.contains(img.getImagePath())) {
                File file = new File(img.getImagePath());
                if (file.exists()) file.delete();
                imageRepository.delete(img);
            }
        }

        Matcher matcher = Pattern.compile("/file/" + group + "/" + category + "/image/temp/([^\"']+)").matcher(content);

        List<String> tempFiles = new ArrayList<>();

        while (matcher.find()) {
            tempFiles.add(matcher.group(1));
        }

        String tempDir = new File("file/" + group + "/" + category + "/image/temp").getAbsolutePath();
        String permanentDir = new File("file/" + group + "/" + category + "/image/" + writeDate).getAbsolutePath();
        File permFolder = new File(permanentDir);
        if (!permFolder.exists()) permFolder.mkdirs();

        for (String fileName : tempFiles) {
            File tempFile = new File(tempDir, fileName);
            File permFile = new File(permFolder, fileName);
            if (tempFile.exists()) {
                try {
                    Files.move(tempFile.toPath(), permFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("파일 이동 실패: " + tempFile.getName(), e);
                }

                // 이미지 정보 추출
                BufferedImage img;
                try {
                    img = ImageIO.read(permFile);
                } catch (IOException e) {
                    throw new RuntimeException("이미지 읽기 실패: " + permFile.getName(), e);
                }
                if (img == null) {
                    throw new RuntimeException("유효하지 않은 이미지 파일: " + permFile.getName());
                }
                long size = permFile.length();
                String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "";

                ImageEntity imgEntity = new ImageEntity();
                imgEntity.setImageSize(size);
                imgEntity.setImageType(extension);
                imgEntity.setImageName(fileName);
                imgEntity.setImagePath("/file/" + group + "/" + category + "/" + writeDate + "/image/" + fileName);
                imgEntity.setDivisionGroup(group);
                imgEntity.setDivisionCategory(category);
                imgEntity.setBoardNum(num);
                imageRepository.save(imgEntity);
            }
        }

        String updatedContent = content.replaceAll("/file/" + group + "/" + category + "/image/temp/",
                "/file/" + group + "/" + category + "/image/" + writeDate + "/");

        return updatedContent;
    }

    @Transactional
    public void deleteImage(String content) {
        if (content == null || content.isBlank()) return;

        Pattern pattern = Pattern.compile("<img[^>]+src=[\"'](/file/.+?)[\"'][^>]*>");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String imagePath = matcher.group(1);

            // DB 삭제
            ImageEntity img = imageRepository.findByImagePath(imagePath);
            if (img != null) {
                imageRepository.delete(img);
            }

            // 로컬 파일 삭제
            File file = new File("." + imagePath);
            if (file.exists()) file.delete();
        }
    }
}