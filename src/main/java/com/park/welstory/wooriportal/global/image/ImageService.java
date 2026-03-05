package com.park.welstory.wooriportal.global.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 이미지 경로 구조
 *   임시: /file/{category}/image/temp/{originalName}  (중복 시 _2, _3 ...)
 *   영구: /file/{category}/image/{originalName}       (중복 시 _2, _3 ...)
 */
@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;

    // ── 중복 파일명 해결 ────────────────────────────────────────────────────
    private String resolveFileName(File dir, String originalName) {
        File candidate = new File(dir, originalName);
        if (!candidate.exists()) return originalName;

        int dot  = originalName.lastIndexOf('.');
        String base = dot >= 0 ? originalName.substring(0, dot) : originalName;
        String ext  = dot >= 0 ? originalName.substring(dot)    : "";

        int seq = 2;
        while (true) {
            String name = base + "_" + seq + ext;
            if (!new File(dir, name).exists()) return name;
            seq++;
        }
    }

    // ── 임시 저장 (에디터 작성 중) ─────────────────────────────────────────
    public Map<String, Object> saveTempImage(MultipartFile image, String group, String category) {
        File tempDir = new File(new File("file/" + category + "/image/temp").getAbsolutePath());
        if (!tempDir.exists()) tempDir.mkdirs();

        String savedName = resolveFileName(tempDir, image.getOriginalFilename());

        try {
            image.transferTo(new File(tempDir, savedName));
        } catch (IOException e) {
            throw new RuntimeException("이미지 임시 저장 실패: " + image.getOriginalFilename(), e);
        }

        String url = "/file/" + category + "/image/temp/" + savedName;
        return Map.of("result", List.of(Map.of("url", url, "name", image.getOriginalFilename())));
    }

    // ── 영구 저장 (게시글 저장 완료 시) ────────────────────────────────────
    public String saveImage(Long num, String content, String group, String category, String writeDate) {

        // 이미 DB에 있는 이미지 중 현재 내용에 없는 것 삭제
        List<ImageEntity> existingImages = imageRepository.findByBoardNumAndDivisionGroup(num, group);
        for (ImageEntity img : existingImages) {
            if (!content.contains(img.getImagePath())) {
                new File(img.getImagePath()).delete();
                imageRepository.delete(img);
            }
        }

        // 임시 경로 패턴에서 파일명 추출
        String tempUrlBase = "/file/" + category + "/image/temp/";
        Matcher matcher = Pattern.compile(Pattern.quote(tempUrlBase) + "([^\"']+)").matcher(content);

        List<String> tempFileNames = new ArrayList<>();
        while (matcher.find()) tempFileNames.add(matcher.group(1));

        File tempDir = new File(new File("file/" + category + "/image/temp").getAbsolutePath());
        File permDir = new File(new File("file/" + category + "/image").getAbsolutePath());
        if (!permDir.exists()) permDir.mkdirs();

        for (String tempName : tempFileNames) {
            File tempFile = new File(tempDir, tempName);
            if (!tempFile.exists()) continue;

            // 영구 경로에서도 중복 해결
            String permName = resolveFileName(permDir, tempName);
            File permFile   = new File(permDir, permName);
            String permUrl  = "/file/" + category + "/image/" + permName;

            try {
                moveWithRetry(tempFile, permFile);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("이미지 이동 실패: " + tempName, e);
            }

            // 이미지 정보 검증
            BufferedImage img;
            try { img = ImageIO.read(permFile); }
            catch (IOException e) { throw new RuntimeException("이미지 읽기 실패: " + permName, e); }
            if (img == null) throw new RuntimeException("유효하지 않은 이미지: " + permName);

            String ext = permName.contains(".") ? permName.substring(permName.lastIndexOf('.') + 1) : "";

            ImageEntity entity = new ImageEntity();
            entity.setImageSize(permFile.length());
            entity.setImageType(ext);
            entity.setImageName(permName);
            entity.setImagePath(permUrl);
            entity.setDivisionGroup(group);
            entity.setDivisionCategory(category);
            entity.setBoardNum(num);
            imageRepository.save(entity);

            // 콘텐츠 내 임시 URL → 영구 URL 교체 (파일명이 바뀔 수 있으므로 개별 처리)
            content = content.replace(tempUrlBase + tempName, permUrl);
        }

        return content;
    }

    // ── 삭제 ────────────────────────────────────────────────────────────────
    @Transactional
    public void deleteImage(String content) {
        if (content == null || content.isBlank()) return;

        Matcher matcher = Pattern.compile("<img[^>]+src=[\"'](/file/.+?)[\"'][^>]*>").matcher(content);
        while (matcher.find()) {
            String path = matcher.group(1);
            ImageEntity img = imageRepository.findByImagePath(path);
            if (img != null) imageRepository.delete(img);
            new File("." + path).delete();
        }
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────────────
    private void moveWithRetry(File src, File dest) throws IOException, InterruptedException {
        for (int i = 0; i < 5; i++) {
            try {
                Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (FileSystemException e) {
                if (i == 4) throw e;
                Thread.sleep(100);
            }
        }
    }
}
