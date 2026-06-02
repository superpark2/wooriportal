package com.mrpark.dev.wooriportal.hrd.session;

import java.time.Instant;
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
 * 프록시 하베스터(mitmproxy 애드온)가 수확한 HRD 세션 쿠키를 받는 머신 연동 엔드포인트.
 *
 * <p>{@code /coolapi/**} 경로라 SecurityConfig 에서 permitAll + CSRF 제외.
 * 선택적으로 공유 시크릿({@code hrd.harvest.token})으로 보호한다.</p>
 */
@RestController
@RequestMapping("/coolapi/hrd")
@RequiredArgsConstructor
public class HrdSessionController {

    private final HrdSessionStore sessionStore;

    /** 설정 시 X-Harvest-Token 헤더와 일치해야 수신. 비어있으면 검사 안 함(PoC). */
    @Value("${hrd.harvest.token:}")
    private String harvestToken;

    @PostMapping("/session")
    public ResponseEntity<?> receive(
            @RequestHeader(value = "X-Harvest-Token", required = false) String token,
            @RequestBody HrdSessionPayload payload) {

        if (harvestToken != null && !harvestToken.isBlank() && !harvestToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid token"));
        }
        if (payload == null || payload.jsessionId() == null || payload.jsessionId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "jsessionId required"));
        }

        boolean changed = sessionStore.update(payload.jsessionId(), payload.wmonid());
        return ResponseEntity.ok(Map.of("ok", true, "changed", changed));
    }

    /** 디버깅용 현재 세션 상태(쿠키 값은 노출하지 않음). */
    @GetMapping("/session/status")
    public Map<String, Object> status() {
        return sessionStore.current()
                .<Map<String, Object>>map(s -> Map.of(
                        "present", true,
                        "harvestedAt", s.getHarvestedAt().toString(),
                        "ageSeconds", Instant.now().getEpochSecond() - s.getHarvestedAt().getEpochSecond()))
                .orElse(Map.of("present", false));
    }

    /** 하베스터가 보내는 페이로드. */
    public record HrdSessionPayload(String jsessionId, String wmonid) {
    }
}
