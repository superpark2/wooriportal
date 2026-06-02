package com.mrpark.dev.wooriportal.hrd.session;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HRD 세션 연동 엔드포인트.
 *
 * <ul>
 *   <li>{@code POST /coolapi/hrd/session} — 하베스터가 수확한 쿠키 주입(머신).</li>
 *   <li>{@code POST /coolapi/hrd/session/takeover} — 웹에서 "끊고 새로 연동" 확인 시 전환창 오픈.</li>
 *   <li>{@code POST /coolapi/hrd/session/disconnect} — 연동 해제.</li>
 *   <li>{@code GET  /coolapi/hrd/session/status} — 연동 상태(소유자/경과).</li>
 * </ul>
 *
 * <p>{@code /coolapi/**} 라 permitAll. 네트워크 노출되므로 {@code hrd.harvest.token} 설정 권장.</p>
 */
@RestController
@RequestMapping("/coolapi/hrd")
@RequiredArgsConstructor
public class HrdSessionController {

    /** 웹 "HRD 연동" 전환창 길이(초). */
    private static final int TAKEOVER_WINDOW_SEC = 90;

    private final HrdSessionStore sessionStore;

    @Value("${hrd.harvest.token:}")
    private String harvestToken;

    @PostMapping("/session")
    public ResponseEntity<?> receive(
            @RequestHeader(value = "X-Harvest-Token", required = false) String token,
            @RequestBody HrdSessionPayload payload) {

        if (!tokenOk(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid token"));
        }
        if (payload == null || payload.jsessionId() == null || payload.jsessionId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "jsessionId required"));
        }

        String source = (payload.source() == null || payload.source().isBlank()) ? "unknown" : payload.source().trim();
        HrdSessionStore.Result result = sessionStore.tryUpdate(payload.jsessionId(), payload.wmonid(), source);

        if (result == HrdSessionStore.Result.REJECTED) {
            // 다른 사용자가 이미 연동 중 — 하베스터는 덮어쓰지 않음
            return ResponseEntity.status(HttpStatus.CONFLICT).body(status("rejected"));
        }
        return ResponseEntity.ok(status("accepted"));
    }

    /**
     * 웹에서 "기존 연동 끊고 새로 연동" 확인 → 전환창 오픈(기존 해제).
     * 자격증명을 싣지 않는 관리 동작이라 토큰 불요(포털 로그인 사용자가 호출).
     */
    @PostMapping("/session/takeover")
    public ResponseEntity<?> takeover() {
        sessionStore.openTakeover(TAKEOVER_WINDOW_SEC);
        return ResponseEntity.ok(Map.of("ok", true, "windowSec", TAKEOVER_WINDOW_SEC));
    }

    @PostMapping("/session/disconnect")
    public ResponseEntity<?> disconnect() {
        sessionStore.disconnect();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/session/status")
    public Map<String, Object> status() {
        return status(null);
    }

    private Map<String, Object> status(String action) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("present", sessionStore.isPresent());
        m.put("takeoverOpen", sessionStore.isTakeoverOpen());
        sessionStore.current().ifPresent(s -> {
            m.put("owner", s.getSource());
            m.put("ageSeconds", Instant.now().getEpochSecond() - s.getHarvestedAt().getEpochSecond());
        });
        if (action != null) {
            m.put("action", action);
        }
        return m;
    }

    private boolean tokenOk(String token) {
        return harvestToken == null || harvestToken.isBlank() || harvestToken.equals(token);
    }

    /** 하베스터가 보내는 페이로드. */
    public record HrdSessionPayload(String jsessionId, String wmonid, String source) {
    }
}
