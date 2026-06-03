package com.mrpark.dev.wooriportal.hrd.board;

import com.mrpark.dev.wooriportal.hrd.HrdNetClient;
import com.mrpark.dev.wooriportal.hrd.HrdSessionExpiredException;
import com.mrpark.dev.wooriportal.hrd.dto.HrdDailyAttendance;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 전광판 데이터 보유/갱신/브로드캐스트.
 *
 * <p>폴러가 {@link #refresh()} 를 호출하면 등록된 과정들의 당일 출결을 HRD 에서 직접 끌어와
 * 정렬된 스냅샷을 만들고 SSE 구독자에게 밀어준다. 모든 전광판이 하나의 스냅샷을 공유한다.</p>
 */
@Slf4j
@Service
public class HrdBoardService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final HrdNetClient client;
    private final HrdRequestTemplateProvider templates;
    private final com.mrpark.dev.wooriportal.hrd.session.HrdSessionStore sessionStore;

    /** 폴링 대상 과정: "tracseId:tracseTme" 콤마 구분. (목록 자동탐색 전까지 설정 기반) */
    @Value("${hrd.board.courses:}")
    private String coursesConfig;

    /** 과정 간 호출 간격(ms) — 정부서버 부하 분산. */
    @Value("${hrd.board.stagger-ms:200}")
    private long staggerMs;

    /** 과정 자동탐색 캐시 갱신 주기(분) — 목록은 하루 내 거의 안 바뀜. */
    @Value("${hrd.board.discover-minutes:10}")
    private long discoverMinutes;

    private volatile List<HrdBoardRow> snapshot = List.of();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private volatile List<CourseKey> discovered;
    private volatile Instant discoveredAt = Instant.EPOCH;

    // 진단용 마지막 갱신 상태
    private volatile Instant lastRefreshAt;
    private volatile String lastStatus = "아직 폴링 전";
    private volatile int lastFailCount;

    public HrdBoardService(HrdNetClient client, HrdRequestTemplateProvider templates,
                           com.mrpark.dev.wooriportal.hrd.session.HrdSessionStore sessionStore) {
        this.client = client;
        this.templates = templates;
        this.sessionStore = sessionStore;
    }

    public List<HrdBoardRow> snapshot() {
        return snapshot;
    }

    /** 폴러가 주기적으로 호출. 세션/템플릿 없으면 조용히 패스. */
    public void refresh() {
        Optional<byte[]> template = templates.detailTemplate();
        if (template.isEmpty()) {
            lastStatus = "요청 템플릿 없음(config/hrd 배치 또는 하베스터 주입 필요)";
            return;
        }
        List<CourseKey> courses = resolveCourses();
        if (courses.isEmpty()) {
            lastStatus = "폴링 대상 과정 없음(자동탐색 실패 + hrd.board.courses 미설정)";
            return;
        }
        String today = LocalDate.now().format(YMD);

        List<HrdDailyAttendance> results = new ArrayList<>();
        int fail = 0;
        for (CourseKey c : courses) {
            try {
                results.add(client.fetchDailyAttendance(template.get(), c.tracseId(), c.tracseTme(), today));
            } catch (HrdSessionExpiredException e) {
                lastStatus = "세션 만료/무효: " + e.getMessage();
                lastFailCount = courses.size();
                lastRefreshAt = Instant.now();
                sessionStore.markBroken(); // 웹에 "끊김" 표시
                log.warn("HRD 세션 만료/무효 — 이번 갱신 중단: {}", e.getMessage());
                return; // 세션 문제는 전 과정 공통 → 중단
            } catch (Exception e) {
                fail++;
                log.warn("과정 {}:{} 조회 실패: {}", c.tracseId(), c.tracseTme(), e.toString());
            }
            sleepQuietly(staggerMs);
        }

        snapshot = sortForBoard(results);
        if (!results.isEmpty()) {
            sessionStore.markHealthy(); // 호출 성공 → 정상
        }
        lastRefreshAt = Instant.now();
        lastFailCount = fail;
        int presentSum = results.stream().mapToInt(HrdDailyAttendance::getPresent).sum();
        lastStatus = String.format("성공 %d과정 / 실패 %d / 출석 %d명", results.size(), fail, presentSum);
        log.info("전광판 갱신: {}", lastStatus);
        broadcast();
    }

    /** 로그인 없이 폴 결과를 확인하는 진단 정보. */
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("courseCount", snapshot.size());
        m.put("lastStatus", lastStatus);
        m.put("lastFailCount", lastFailCount);
        m.put("lastRefreshAt", lastRefreshAt != null ? lastRefreshAt.toString() : null);
        m.put("courses", snapshot.stream().map(r ->
                Map.of("name", String.valueOf(r.getCourseName()),
                        "present", r.getPresent(), "late", r.getLate(),
                        "absent", r.getAbsent(), "total", r.getTotal(),
                        "checkedOut", r.getCheckedOut(), "allCheckedOut", r.isAllCheckedOut())).toList());
        return m;
    }

    /** 진행중 과정 먼저, 전원 퇴실(종료) 과정은 뒤로. */
    private List<HrdBoardRow> sortForBoard(List<HrdDailyAttendance> results) {
        return results.stream()
                .sorted(Comparator
                        .comparing(HrdDailyAttendance::isAllCheckedOut)              // false(진행중) 먼저
                        .thenComparing(a -> -a.getPresent()))                        // 출석 많은 순
                .map(HrdBoardRow::new)
                .toList();
    }

    // ── SSE ──

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // 무제한
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    private void broadcast() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
            } catch (Exception e) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }

    // ── helpers ──

    /** 폴링 대상 과정 결정: 목록 템플릿 있으면 오늘 과정 자동탐색(캐시), 없으면 설정값. */
    private List<CourseKey> resolveCourses() {
        Optional<byte[]> listT = templates.listTemplate();
        if (listT.isPresent()) {
            boolean stale = discovered == null
                    || java.time.Duration.between(discoveredAt, Instant.now()).toMinutes() >= discoverMinutes;
            if (stale) {
                try {
                    List<CourseKey> found = new ArrayList<>();
                    for (var c : client.fetchCourseList(listT.get())) {
                        if (c.getTracseId() != null && c.getTracseTme() != null) {
                            found.add(new CourseKey(c.getTracseId(), c.getTracseTme()));
                        }
                    }
                    if (!found.isEmpty()) {
                        discovered = found;
                        discoveredAt = Instant.now();
                        log.info("오늘 과정 자동탐색: {}개", found.size());
                    }
                } catch (Exception e) {
                    log.warn("과정 자동탐색 실패(설정값으로 폴백): {}", e.toString());
                }
            }
            if (discovered != null && !discovered.isEmpty()) {
                return discovered;
            }
        }
        return parseCourses(); // 폴백: hrd.board.courses
    }

    private List<CourseKey> parseCourses() {
        List<CourseKey> list = new ArrayList<>();
        if (coursesConfig == null || coursesConfig.isBlank()) {
            return list;
        }
        for (String token : coursesConfig.split(",")) {
            String[] parts = token.trim().split(":");
            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                list.add(new CourseKey(parts[0].trim(), parts[1].trim()));
            }
        }
        return list;
    }

    private static void sleepQuietly(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record CourseKey(String tracseId, String tracseTme) {
    }
}
