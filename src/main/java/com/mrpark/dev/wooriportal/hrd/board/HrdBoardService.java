package com.mrpark.dev.wooriportal.hrd.board;

import com.mrpark.dev.wooriportal.hrd.HrdNetClient;
import com.mrpark.dev.wooriportal.hrd.HrdSessionExpiredException;
import com.mrpark.dev.wooriportal.hrd.dto.HrdDailyAttendance;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    /** 폴링 대상 과정: "tracseId:tracseTme" 콤마 구분. (목록 자동탐색 전까지 설정 기반) */
    @Value("${hrd.board.courses:}")
    private String coursesConfig;

    /** 과정 간 호출 간격(ms) — 정부서버 부하 분산. */
    @Value("${hrd.board.stagger-ms:200}")
    private long staggerMs;

    private volatile List<HrdBoardRow> snapshot = List.of();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public HrdBoardService(HrdNetClient client, HrdRequestTemplateProvider templates) {
        this.client = client;
        this.templates = templates;
    }

    public List<HrdBoardRow> snapshot() {
        return snapshot;
    }

    /** 폴러가 주기적으로 호출. 세션/템플릿 없으면 조용히 패스. */
    public void refresh() {
        Optional<byte[]> template = templates.detailTemplate();
        if (template.isEmpty()) {
            return; // 템플릿 미배치 — 로그는 provider 가 1회 남김
        }
        List<CourseKey> courses = parseCourses();
        if (courses.isEmpty()) {
            return;
        }
        String today = LocalDate.now().format(YMD);

        List<HrdDailyAttendance> results = new ArrayList<>();
        for (CourseKey c : courses) {
            try {
                results.add(client.fetchDailyAttendance(template.get(), c.tracseId(), c.tracseTme(), today));
            } catch (HrdSessionExpiredException e) {
                log.warn("HRD 세션 만료/무효 — 이번 갱신 중단: {}", e.getMessage());
                return; // 세션 문제는 전 과정 공통 → 중단
            } catch (Exception e) {
                log.warn("과정 {}:{} 조회 실패: {}", c.tracseId(), c.tracseTme(), e.toString());
            }
            sleepQuietly(staggerMs);
        }

        snapshot = sortForBoard(results);
        broadcast();
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
