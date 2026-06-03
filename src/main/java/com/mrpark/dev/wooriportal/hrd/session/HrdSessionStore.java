package com.mrpark.dev.wooriportal.hrd.session;

import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 현재 살아있는 HRD 세션을 보관하는 싱글톤 저장소.
 *
 * <p>여러 PC 의 하베스터가 동시에 밀어넣을 수 있으므로 <b>소유권</b> 개념을 둔다:
 * 한 번 누군가(source) 가 연동되면, 다른 source 의 갱신은 거부한다(서로 덮어쓰며
 * 충돌하는 것 방지). 웹의 "HRD 연동" 버튼이 전환 창({@link #openTakeover})을 열면
 * 그 시간 동안의 첫 갱신이 소유권을 가져간다.</p>
 */
@Slf4j
@Component
public class HrdSessionStore {

    private volatile HrdSession current;
    /** 전환 창 마감시각 — 이 시각 전에는 다른 source 도 소유권을 가져갈 수 있다. */
    private volatile Instant takeoverDeadline = Instant.EPOCH;
    /** 직전 호출이 세션 만료로 거부됨(쿠키는 있으나 죽음) → 웹에 "끊김" 표시. */
    private volatile boolean broken;

    public enum Result { ACCEPTED, REJECTED }

    /**
     * 하베스트된 쿠키로 갱신 시도.
     *
     * @return ACCEPTED(반영됨) 또는 REJECTED(다른 사용자가 이미 연동 중)
     */
    public synchronized Result tryUpdate(String jsessionId, String wmonid, String source) {
        if (jsessionId == null || jsessionId.isBlank()) {
            return Result.REJECTED;
        }
        boolean owned = current != null;
        boolean sameOwner = owned && java.util.Objects.equals(current.getSource(), source);
        boolean windowOpen = Instant.now().isBefore(takeoverDeadline);

        if (owned && !sameOwner && !windowOpen) {
            return Result.REJECTED; // 다른 사람이 연동 중 + 전환창 닫힘
        }

        boolean ownerChanged = !owned || !sameOwner;
        current = new HrdSession(jsessionId, wmonid, source, Instant.now());
        takeoverDeadline = Instant.EPOCH; // 소비
        broken = false;                   // 새 쿠키 들어옴 → 정상 복구
        if (ownerChanged) {
            log.info("HRD 연동: source={} JSESSIONID=...{}", source, tail(jsessionId));
        }
        return Result.ACCEPTED;
    }

    /** 호출이 세션 만료로 거부됨 — 쿠키는 유지하되 "끊김"으로 표시(재로그인 시 자동 복구). */
    public void markBroken() {
        if (current != null && !broken) {
            broken = true;
            log.warn("HRD 세션 끊김(만료) — source={}", current.getSource());
        }
    }

    /** 호출 성공 — 정상 상태. */
    public void markHealthy() {
        broken = false;
    }

    public boolean isBroken() {
        return broken;
    }

    /** 전환 창을 연다(웹에서 "끊고 새로 연동" 확인 시). 기존 연동은 즉시 해제. */
    public synchronized void openTakeover(int seconds) {
        log.info("HRD 연동 전환 창 열림 ({}초) — 기존 연동 해제", seconds);
        current = null;
        broken = false;
        takeoverDeadline = Instant.now().plusSeconds(seconds);
    }

    /** 연동 해제. */
    public synchronized void disconnect() {
        current = null;
        broken = false;
        takeoverDeadline = Instant.EPOCH;
        log.info("HRD 연동 해제");
    }

    public Optional<HrdSession> current() {
        return Optional.ofNullable(current);
    }

    public boolean isPresent() {
        return current != null;
    }

    public boolean isTakeoverOpen() {
        return Instant.now().isBefore(takeoverDeadline);
    }

    private static String tail(String s) {
        return s.length() <= 6 ? s : s.substring(s.length() - 6);
    }
}
