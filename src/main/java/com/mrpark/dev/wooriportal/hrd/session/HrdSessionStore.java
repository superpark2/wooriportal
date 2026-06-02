package com.mrpark.dev.wooriportal.hrd.session;

import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 현재 살아있는 HRD 세션을 보관하는 싱글톤 저장소.
 *
 * <p>프록시 하베스터가 새 쿠키를 밀어넣으면 갱신되고, HRD 클라이언트/폴러가 읽어 쓴다.
 * 단일 세션을 모든 전광판이 공유한다(서버 부하 최소화).</p>
 */
@Slf4j
@Component
public class HrdSessionStore {

    private volatile HrdSession current;

    /**
     * 하베스트된 쿠키로 갱신한다. 자격증명이 바뀐 경우에만 로그를 남긴다.
     *
     * @return 실제로 갱신되었으면 true(신규/변경), 동일 값이면 false
     */
    public boolean update(String jsessionId, String wmonid) {
        if (jsessionId == null || jsessionId.isBlank()) {
            return false;
        }
        HrdSession existing = current;
        boolean changed = existing == null || !existing.sameCredentials(jsessionId, wmonid);
        current = new HrdSession(jsessionId, wmonid, Instant.now());
        if (changed) {
            log.info("HRD 세션 갱신: JSESSIONID=...{} WMONID={}", tail(jsessionId), wmonid);
        }
        return changed;
    }

    public Optional<HrdSession> current() {
        return Optional.ofNullable(current);
    }

    public boolean isPresent() {
        return current != null;
    }

    private static String tail(String s) {
        return s.length() <= 6 ? s : s.substring(s.length() - 6);
    }
}
