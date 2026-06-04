package com.mrpark.dev.wooriportal.jobinfo.employment24.controller;

import com.mrpark.dev.wooriportal.jobinfo.employment24.dto.Emp24Request;
import com.mrpark.dev.wooriportal.jobinfo.employment24.dto.Emp24Result;
import com.mrpark.dev.wooriportal.jobinfo.employment24.service.Employment24CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class Employment24Controller {

    private final Employment24CrawlerService crawlerService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/jobinfo/employment24")
    public String index() { return "jobinfo/employment24/index"; }

    @PostMapping("/jobinfo/employment24/api/search")
    @ResponseBody
    public ResponseEntity<Emp24Result> search(@RequestBody Emp24Request req) {
        return ResponseEntity.ok(crawlerService.crawl(req));
    }

    /** SSE 진행률 스트림 */
    @GetMapping(value = "/jobinfo/employment24/api/search-all/progress/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter progress(@PathVariable String jobId) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(()   -> emitters.remove(jobId));
        return emitter;
    }

    /** 전체 수집 시작 → jobId 반환 */
    @PostMapping("/jobinfo/employment24/api/search-all")
    @ResponseBody
    public ResponseEntity<Map<String, String>> searchAll(@RequestBody Emp24Request req,
                                                         @RequestParam(defaultValue = "0") int maxPages) {
        String jobId = UUID.randomUUID().toString();

        executor.submit(() -> {
            SseEmitter emitter = null;
            try {
                for (int i = 0; i < 20; i++) {
                    emitter = emitters.get(jobId);
                    if (emitter != null) break;
                    Thread.sleep(100);
                }
                final SseEmitter fe = emitter;

                Emp24Result result = crawlerService.crawlAllWithProgress(
                        req, maxPages, jobId,
                        (cur, tot, cnt) -> {
                            if (fe == null) return;
                            // 클라이언트 연결 종료/취소 후엔 emitter가 닫혀 있을 수 있음 → 모든 예외 무시
                            try {
                                fe.send(SseEmitter.event().name("progress")
                                        .data(Map.of("currentPage", cur, "totalPages", tot, "collectedCount", cnt)));
                            } catch (Exception e) { /* client disconnected / completed */ }
                        }
                );

                // 취소된 작업이면 done 전송 생략 (emitter는 cancel()에서 이미 정리됨)
                if (fe != null && !crawlerService.isCancelled(jobId)) {
                    try { fe.send(SseEmitter.event().name("done").data(result)); fe.complete(); }
                    catch (Exception e) { /* client disconnected / completed */ }
                }
            } catch (Exception e) {
                log.error("고용24 crawlAll 오류: {}", e.getMessage());
                if (emitter != null) {
                    try { emitter.send(SseEmitter.event().name("error").data(e.getMessage())); emitter.complete(); }
                    catch (Exception ex) { /* ignore */ }
                }
            } finally {
                crawlerService.cleanupJobPublic(jobId);
            }
        });

        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    /** 취소 */
    @DeleteMapping("/jobinfo/employment24/api/search-all/{jobId}")
    @ResponseBody
    public ResponseEntity<Void> cancel(@PathVariable String jobId) {
        crawlerService.cancelJob(jobId);
        // 클라이언트는 이미 EventSource를 닫았으므로 추가 이벤트 전송 없이 조용히 정리한다.
        SseEmitter e = emitters.remove(jobId);
        if (e != null) {
            try { e.complete(); } catch (Exception ex) { /* ignore */ }
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/jobinfo/employment24/api/build-url")
    @ResponseBody
    public ResponseEntity<String> buildUrl(@RequestBody Emp24Request req) {
        return ResponseEntity.ok(crawlerService.buildSearchUrl(req));
    }

    /** 직종(직업분류) 트리 — superJobsCd 비면 대분류, 값 있으면 하위 분류 */
    @GetMapping("/jobinfo/employment24/api/jobs")
    @ResponseBody
    public ResponseEntity<java.util.List<com.mrpark.dev.wooriportal.jobinfo.employment24.dto.Emp24JobCategory>> jobs(
            @RequestParam(required = false, defaultValue = "") String superJobsCd) {
        return ResponseEntity.ok(crawlerService.fetchJobCategories(superJobsCd));
    }
}
