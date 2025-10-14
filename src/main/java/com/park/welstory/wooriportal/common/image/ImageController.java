package com.park.welstory.wooriportal.common.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RequiredArgsConstructor
@Controller
public class ImageController {

    private final ImageService imageService;

    // 선에디터용 이미지 임시 업로드
    @PostMapping("/{group}/{category}/image/upload")
    @ResponseBody
    public Map<String, Object> uploadImage(@RequestParam("file-0") MultipartFile image,
                                           @PathVariable String group, @PathVariable String category) {
        if (image.isEmpty()) {
            throw new RuntimeException("파일이 없습니다 (이미지 컨트롤러)");
        }

        return imageService.saveTempImage(image, group, category);
    }

}