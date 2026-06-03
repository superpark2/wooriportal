package com.mrpark.dev.wooriportal.hrd.board;

import com.mrpark.dev.wooriportal.hrd.ssv.SsvData;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvDataset;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvDecoder;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HRD 요청 본문 템플릿(인증서·gds_userInfo 포함) 공급/보관.
 *
 * <p>영속 저장은 DB({@link HrdRequestTemplateEntity}). 우선순위는 DB → 시드 파일.
 * 프록시 하베스터가 실제 HRD 요청을 push 하면({@link #save}) DB 가 갱신되어,
 * 특정 개인 인증서 만료에 묶이지 않고 "최근 HRD 사용자"의 자격으로 동작한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrdRequestTemplateProvider {

    public static final String DETAIL = "detail";
    public static final String LIST = "list";

    private final HrdRequestTemplateRepository repository;

    /** 최초 시드용 파일 경로(DB 비어있을 때만 사용). */
    @Value("${hrd.template.detail-path:config/hrd/selectDailAtndceDetail.req.hex}")
    private String detailSeedPath;
    @Value("${hrd.template.list-path:config/hrd/selectAtendList.req.hex}")
    private String listSeedPath;

    private final Map<String, byte[]> cache = new LinkedHashMap<>();
    private final Map<String, String> owners = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        loadOrSeed(DETAIL, detailSeedPath, "상세(selectDailAtndceDetail)");
        loadOrSeed(LIST, listSeedPath, "목록(selectAtendList)");
    }

    private void loadOrSeed(String endpoint, String seedPath, String label) {
        Optional<HrdRequestTemplateEntity> row = repository.findById(endpoint);
        if (row.isPresent()) {
            cache.put(endpoint, row.get().getBody());
            owners.put(endpoint, row.get().getOwnerName());
            log.info("HRD {} 템플릿 DB 로드: 소유자={} ({} bytes)", label, row.get().getOwnerName(), row.get().getBody().length);
            return;
        }
        // DB 비어있으면 시드 파일로 1회 적재
        try {
            Path p = Path.of(seedPath);
            if (Files.isReadable(p)) {
                byte[] body = parseHex(Files.readString(p, StandardCharsets.UTF_8));
                save(endpoint, body, "seed-file");
                log.info("HRD {} 템플릿 시드파일 적재→DB: {} ({} bytes)", label, seedPath, body.length);
            } else {
                log.warn("HRD {} 템플릿 없음(DB·파일 모두) — 하베스터 push 또는 파일 배치 필요", label);
            }
        } catch (Exception e) {
            log.warn("HRD {} 템플릿 시드 실패: {}", label, e.getMessage());
        }
    }

    public Optional<byte[]> detailTemplate() {
        return Optional.ofNullable(cache.get(DETAIL));
    }

    public Optional<byte[]> listTemplate() {
        return Optional.ofNullable(cache.get(LIST));
    }

    /** 하베스터/운영자가 요청 본문 템플릿을 저장(인증서 포함). DB 영속 + 캐시 갱신. */
    public void save(String endpoint, byte[] body, String source) {
        String owner = extractOwner(body);
        HrdRequestTemplateEntity e = repository.findById(endpoint).orElseGet(HrdRequestTemplateEntity::new);
        e.setEndpoint(endpoint);
        e.setBody(body);
        e.setOwnerName(owner);
        e.setSource(source);
        repository.save(e);
        cache.put(endpoint, body);
        owners.put(endpoint, owner);
        log.info("HRD {} 템플릿 저장: 소유자={} source={} ({} bytes)", endpoint, owner, source, body.length);
    }

    /** 템플릿 보유 현황(웹 표시용). */
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String ep : new String[]{DETAIL, LIST}) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("present", cache.containsKey(ep));
            t.put("owner", owners.get(ep));
            repository.findById(ep).ifPresent(r -> {
                t.put("source", r.getSource());
                t.put("updatedAt", r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : null);
            });
            m.put(ep, t);
        }
        return m;
    }

    /** gds_userInfo.usrNm 에서 인증서 소유자 이름 추출. */
    private static String extractOwner(byte[] body) {
        try {
            SsvData data = SsvDecoder.decode(body);
            SsvDataset u = data.getDataset("gds_userInfo");
            if (u != null && u.getRowCount() > 0) {
                String nm = u.getString(0, "usrNm");
                return nm != null ? nm : "unknown";
            }
        } catch (Exception ignore) {
            // 디코드 실패 시 무시
        }
        return "unknown";
    }

    private static byte[] parseHex(String hex) {
        String[] tokens = hex.trim().split("\\s+");
        byte[] out = new byte[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            out[i] = (byte) Integer.parseInt(tokens[i], 16);
        }
        return out;
    }

    // 사용 안 함(하위호환): 과거 setter
    public void setDetailTemplate(byte[] body) {
        save(DETAIL, body, "manual");
    }

    public void setListTemplate(byte[] body) {
        save(LIST, body, "manual");
    }
}
