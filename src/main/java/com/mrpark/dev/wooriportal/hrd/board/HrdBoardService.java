package com.mrpark.dev.wooriportal.hrd.board;

import com.mrpark.dev.wooriportal.hrd.HrdNetClient;
import com.mrpark.dev.wooriportal.hrd.HrdSessionExpiredException;
import com.mrpark.dev.wooriportal.hrd.dto.HrdDailyAttendance;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    /** 폴링 ON/OFF 스위치(전광판 좌상단 버튼). OFF 면 HRD 호출 안 함. */
    @Value("${hrd.board.enabled:true}")
    private volatile boolean enabled;

    private volatile List<HrdBoardRow> snapshot = List.of();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private volatile List<CourseKey> discovered;
    private volatile Instant discoveredAt = Instant.EPOCH;

    // 진단용 마지막 갱신 상태
    private volatile Instant lastRefreshAt;
    private volatile String lastStatus = "아직 폴링 전";
    private volatile int lastFailCount;

    private final HrdCourseScheduleRepository scheduleRepo;

    public HrdBoardService(HrdNetClient client, HrdRequestTemplateProvider templates,
                           com.mrpark.dev.wooriportal.hrd.session.HrdSessionStore sessionStore,
                           HrdCourseScheduleRepository scheduleRepo) {
        this.client = client;
        this.templates = templates;
        this.sessionStore = sessionStore;
        this.scheduleRepo = scheduleRepo;
    }

    public List<HrdBoardRow> snapshot() {
        return snapshot;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** 폴링 ON/OFF. OFF 로 끄면 다음 틱부터 HRD 호출 중단(전광판은 마지막 스냅샷 유지). */
    public void setEnabled(boolean on) {
        this.enabled = on;
        log.info("전광판 폴링 {}", on ? "ON" : "OFF");
        if (!on) {
            lastStatus = "중지됨(OFF)";
        }
        broadcastState();
    }

    /** 폴러가 주기적으로 호출. OFF/세션·템플릿 없으면 조용히 패스. */
    public void refresh() {
        if (!enabled) {
            return; // 폴링 OFF
        }
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
        int presentSum = snapshot.stream().mapToInt(HrdBoardRow::getPresent).sum();
        lastStatus = String.format("성공 %d과정 / 실패 %d / 출석 %d명", results.size(), fail, presentSum);
        log.info("전광판 갱신: {}", lastStatus);
        broadcast();
    }

    /** 로그인 없이 폴 결과를 확인하는 진단 정보. */
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", enabled);
        m.put("courseCount", snapshot.size());
        m.put("lastStatus", lastStatus);
        m.put("lastFailCount", lastFailCount);
        m.put("lastRefreshAt", lastRefreshAt != null ? lastRefreshAt.toString() : null);
        m.put("courses", snapshot.stream().map(r ->
                Map.of("name", String.valueOf(r.getCourseName()),
                        "present", r.getPresent(), "late", r.getLate(),
                        "absent", r.getAbsent(), "waiting", r.getWaiting(), "total", r.getTotal(),
                        "checkedOut", r.getCheckedOut(), "allCheckedOut", r.isAllCheckedOut())).toList());
        return m;
    }

    /** 규칙 판정 + 강의요일/특이사항 적용 후 정렬: 강의일·진행중 먼저, 비강의일/전원퇴실은 뒤로. */
    private List<HrdBoardRow> sortForBoard(List<HrdDailyAttendance> results) {
        Map<String, HrdCourseScheduleEntity> schedules = loadSchedules();
        int todayDow = LocalDate.now().getDayOfWeek().getValue(); // 1=월..7=일
        LocalTime now = LocalTime.now();

        List<HrdBoardRow> rows = new ArrayList<>();
        for (HrdDailyAttendance a : results) {
            String id = a.getCourse() != null ? a.getCourse().getTracseId() : null;
            String tme = a.getCourse() != null ? a.getCourse().getTracseTme() : null;
            HrdCourseScheduleEntity sch = schedules.get(HrdCourseScheduleEntity.key(id, tme));
            List<Integer> days = sch != null ? parseDays(sch.getDaysOfWeek()) : List.of();
            String notes = sch != null ? sch.getNotes() : null;
            Integer lunch = sch != null ? sch.getLunchMinutes() : null;
            boolean classDay = days.isEmpty() || days.contains(todayDow);
            rows.add(new HrdBoardRow(a, classDay, now, days, notes, lunch));
        }
        rows.sort(Comparator
                .comparing(HrdBoardRow::isClassDay).reversed()        // 강의일 먼저
                .thenComparing(HrdBoardRow::isAllCheckedOut)          // 진행중 먼저
                .thenComparing(r -> -(r.getPresent() + r.getLate()))); // 출석 많은 순
        return rows;
    }

    private Map<String, HrdCourseScheduleEntity> loadSchedules() {
        Map<String, HrdCourseScheduleEntity> map = new LinkedHashMap<>();
        for (HrdCourseScheduleEntity e : scheduleRepo.findAll()) {
            map.put(e.getCourseKey(), e);
        }
        return map;
    }

    static List<Integer> parseDays(String csv) {
        List<Integer> days = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return days;
        }
        for (String t : csv.split(",")) {
            try {
                int d = Integer.parseInt(t.trim());
                if (d >= 1 && d <= 7) {
                    days.add(d);
                }
            } catch (NumberFormatException ignore) {
                // skip
            }
        }
        return days;
    }

    private HrdCourseScheduleEntity upsert(String tracseId, String tracseTme) {
        String key = HrdCourseScheduleEntity.key(tracseId, tracseTme);
        HrdCourseScheduleEntity e = scheduleRepo.findById(key).orElseGet(HrdCourseScheduleEntity::new);
        e.setCourseKey(key);
        e.setTracseId(tracseId);
        e.setTracseTme(tracseTme);
        e.setLastSeenAt(LocalDateTime.now());
        return e;
    }

    /** 과정 강의요일 + 점심(분) 저장(전체 공유). days 빈 리스트면 매일, lunchMinutes null이면 시간기반 기본. */
    public void saveSchedule(String tracseId, String tracseTme, List<Integer> days, Integer lunchMinutes) {
        HrdCourseScheduleEntity e = upsert(tracseId, tracseTme);
        e.setDaysOfWeek(days == null ? "" : days.stream().map(String::valueOf).reduce((x, y) -> x + "," + y).orElse(""));
        e.setLunchMinutes(lunchMinutes);
        scheduleRepo.save(e);
        log.info("강의설정 저장: {} 요일={} 점심={}분", e.getCourseKey(), e.getDaysOfWeek(), lunchMinutes);
    }

    /** 과정 특이사항 저장(전체 공유). */
    public void saveNote(String tracseId, String tracseTme, String note) {
        HrdCourseScheduleEntity e = upsert(tracseId, tracseTme);
        e.setNotes(note == null ? "" : note.trim());
        scheduleRepo.save(e);
        log.info("특이사항 저장: {}", e.getCourseKey());
    }

    public Map<String, String> schedules() {
        Map<String, String> m = new LinkedHashMap<>();
        for (HrdCourseScheduleEntity e : scheduleRepo.findAll()) {
            m.put(e.getCourseKey(), e.getDaysOfWeek());
        }
        return m;
    }

    /** 활성 과정의 종료일/최종확인 갱신(자동삭제 기준). 설정 행이 있는 과정만. */
    private void touchSchedules(List<com.mrpark.dev.wooriportal.hrd.dto.HrdCourse> courses) {
        for (var c : courses) {
            String key = HrdCourseScheduleEntity.key(c.getTracseId(), c.getTracseTme());
            scheduleRepo.findById(key).ifPresent(e -> {
                e.setTracseEndDe(c.getTracseEndDe());
                e.setLastSeenAt(LocalDateTime.now());
                scheduleRepo.save(e);
            });
        }
    }

    /** 끝난 과정 설정 자동 삭제: 종료일+7일 경과 또는 14일간 미확인. 매일 03:10. */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 10 3 * * *")
    public void cleanupEndedSchedules() {
        String cutoff = LocalDate.now().minusDays(7).format(YMD);
        LocalDateTime stale = LocalDateTime.now().minusDays(14);
        List<HrdCourseScheduleEntity> all = scheduleRepo.findAll();
        List<HrdCourseScheduleEntity> toDelete = new ArrayList<>();
        for (HrdCourseScheduleEntity e : all) {
            boolean ended = e.getTracseEndDe() != null && e.getTracseEndDe().length() == 8
                    && e.getTracseEndDe().compareTo(cutoff) < 0;
            boolean unseen = e.getLastSeenAt() != null && e.getLastSeenAt().isBefore(stale);
            if (ended || unseen) {
                toDelete.add(e);
            }
        }
        if (!toDelete.isEmpty()) {
            scheduleRepo.deleteAll(toDelete);
            log.info("끝난 과정 설정 {}건 자동삭제", toDelete.size());
        }
    }

    // ── SSE ──

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // 무제한
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("state").data(Map.of("enabled", enabled)));
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

    private void broadcastState() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("state").data(Map.of("enabled", enabled)));
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
                    var courses = client.fetchCourseList(listT.get());
                    List<CourseKey> found = new ArrayList<>();
                    for (var c : courses) {
                        if (c.getTracseId() != null && c.getTracseTme() != null) {
                            found.add(new CourseKey(c.getTracseId(), c.getTracseTme()));
                        }
                    }
                    if (!found.isEmpty()) {
                        discovered = found;
                        discoveredAt = Instant.now();
                        touchSchedules(courses); // 종료일/최종확인 갱신(자동삭제 기준)
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
