package com.park.welstory.wooriportal.global.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RequiredArgsConstructor
@Controller
public class ImageController {

    private final ImageService imageService;

    // SunEditor: 필드명 "file-0", 응답 { "result": [{ "url": "...", "name": "..." }] }
    @PostMapping("/{group}/{category}/image/upload")
    @ResponseBody
    public Map<String, Object> uploadImage(
            @RequestParam("file-0") MultipartFile image,
            @PathVariable String group,
            @PathVariable String category) {

        if (image.isEmpty()) {
            return Map.of("errorMessage", "파일이 없습니다.");
        }

        try {
            return imageService.saveTempImage(image, group, category);
        } catch (Exception e) {
            return Map.of("errorMessage", "이미지 업로드 실패: " + e.getMessage());
        }
    }
}
