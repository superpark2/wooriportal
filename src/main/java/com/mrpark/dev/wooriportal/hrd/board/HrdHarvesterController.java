package com.mrpark.dev.wooriportal.hrd.board;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 하베스터 연동: 배포 zip 다운로드 + 요청 템플릿(인증서 포함) 수신.
 * {@code /coolapi/**} 라 permitAll. 네트워크 노출 보호용으로 {@code hrd.harvest.token} 권장.
 */
@RestController
@RequestMapping("/coolapi/hrd/harvester")
@RequiredArgsConstructor
public class HrdHarvesterController {

    private static final String[] FILES = {"hrd_harvest.py", "start-harvester.bat", "README.md"};
    private static final Set<String> ENDPOINTS = Set.of(HrdRequestTemplateProvider.DETAIL, HrdRequestTemplateProvider.LIST);

    private final HrdRequestTemplateProvider templates;

    @Value("${hrd.harvest.token:}")
    private String harvestToken;

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

    /**
     * 하베스터가 가로챈 실제 HRD 요청 본문(인증서 포함)을 저장한다.
     * 이 본문이 인증의 핵심이라, 최근 HRD 사용자의 요청이 곧 활성 템플릿이 된다.
     */
    @PostMapping(value = "/template", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<?> receiveTemplate(
            @RequestHeader(value = "X-Harvest-Token", required = false) String token,
            @RequestParam("endpoint") String endpoint,
            @RequestBody byte[] body) {

        if (harvestToken != null && !harvestToken.isBlank() && !harvestToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid token"));
        }
        if (!ENDPOINTS.contains(endpoint)) {
            return ResponseEntity.badRequest().body(Map.of("error", "endpoint must be 'detail' or 'list'"));
        }
        if (body == null || body.length < 2 || (body[0] & 0xFF) != 0xFF || (body[1] & 0xFF) != 0xAD) {
            return ResponseEntity.badRequest().body(Map.of("error", "not an SSV body (FF AD)"));
        }
        templates.save(endpoint, body, "harvester");
        return ResponseEntity.ok(templates.status().get(endpoint));
    }

    /** 템플릿 보유/소유자 현황(로그인 없이 확인). */
    @GetMapping("/template/status")
    public Map<String, Object> templateStatus() {
        return templates.status();
    }
}
