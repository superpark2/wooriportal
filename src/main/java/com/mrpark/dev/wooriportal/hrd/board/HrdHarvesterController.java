package com.mrpark.dev.wooriportal.hrd.board;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HRD 세션 하베스터 배포 — 직원이 자기 PC 에 받아 실행할 .bat/.py 묶음을 내려준다.
 * {@code /coolapi/**} 라 permitAll.
 */
@RestController
@RequestMapping("/coolapi/hrd/harvester")
public class HrdHarvesterController {

    private static final String[] FILES = {
            "hrd_harvest.py", "start-harvester.bat", "README.md"
    };

    @GetMapping("/download")
    public ResponseEntity<byte[]> download() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            for (String name : FILES) {
                ClassPathResource res = new ClassPathResource("hrd/harvester/" + name);
                if (!res.exists()) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(name));
                try (InputStream in = res.getInputStream()) {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename("hrd-harvester.zip").build());
        return ResponseEntity.ok().headers(headers).body(baos.toByteArray());
    }
}
