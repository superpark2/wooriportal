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

    @Value("${hrd.template.list-path:config/hrd/selectAtendList.req.hex}")
    private String listPath;

    private volatile byte[] detailTemplate;
    private volatile byte[] listTemplate;

    @PostConstruct
    void loadFromFile() {
        detailTemplate = load(detailPath, "상세(selectDailAtndceDetail)");
        listTemplate = load(listPath, "목록(selectAtendList)");
    }

    private byte[] load(String path, String label) {
        try {
            Path p = Path.of(path);
            if (Files.isReadable(p)) {
                byte[] b = parseHex(Files.readString(p, StandardCharsets.UTF_8));
                log.info("HRD {} 요청 템플릿 로드: {} ({} bytes)", label, path, b.length);
                return b;
            }
            log.warn("HRD {} 요청 템플릿 없음: {} — 파일 배치 필요", label, path);
        } catch (Exception e) {
            log.warn("HRD {} 템플릿 로드 실패: {}", label, e.getMessage());
        }
        return null;
    }

    public Optional<byte[]> detailTemplate() {
        return Optional.ofNullable(detailTemplate);
    }

    public Optional<byte[]> listTemplate() {
        return Optional.ofNullable(listTemplate);
    }

    /** 하베스터/운영자가 런타임에 템플릿을 주입. */
    public void setDetailTemplate(byte[] body) {
        this.detailTemplate = body;
    }

    public void setListTemplate(byte[] body) {
        this.listTemplate = body;
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
