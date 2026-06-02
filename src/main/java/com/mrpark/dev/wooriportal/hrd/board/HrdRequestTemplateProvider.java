package com.mrpark.dev.wooriportal.hrd.board;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HRD 요청 본문 템플릿(인증서·gds_userInfo 포함)을 공급한다.
 *
 * <p>템플릿은 PII 라 git 에 두지 않고 외부 파일(기본 {@code config/hrd/selectDailAtndceDetail.req.hex})
 * 에서 읽는다. 추후 프록시 하베스터가 런타임에 주입({@link #setDetailTemplate})할 수도 있다.</p>
 */
@Slf4j
@Component
public class HrdRequestTemplateProvider {

    @Value("${hrd.template.detail-path:config/hrd/selectDailAtndceDetail.req.hex}")
    private String detailPath;

    private volatile byte[] detailTemplate;

    @PostConstruct
    void loadFromFile() {
        try {
            Path p = Path.of(detailPath);
            if (Files.isReadable(p)) {
                detailTemplate = parseHex(Files.readString(p, StandardCharsets.UTF_8));
                log.info("HRD 상세 요청 템플릿 로드: {} ({} bytes)", detailPath, detailTemplate.length);
            } else {
                log.warn("HRD 상세 요청 템플릿 없음: {} — 하베스터 주입 또는 파일 배치 필요", detailPath);
            }
        } catch (Exception e) {
            log.warn("HRD 템플릿 로드 실패: {}", e.getMessage());
        }
    }

    public Optional<byte[]> detailTemplate() {
        return Optional.ofNullable(detailTemplate);
    }

    /** 하베스터/운영자가 런타임에 템플릿을 주입. */
    public void setDetailTemplate(byte[] body) {
        this.detailTemplate = body;
    }

    private static byte[] parseHex(String hex) {
        String[] tokens = hex.trim().split("\\s+");
        byte[] out = new byte[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            out[i] = (byte) Integer.parseInt(tokens[i], 16);
        }
        return out;
    }
}
