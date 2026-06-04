package com.mrpark.dev.wooriportal.jobinfo.jobkorea.controller;

import com.mrpark.dev.wooriportal.jobinfo.jobkorea.dto.JkRequest;
import com.mrpark.dev.wooriportal.jobinfo.jobkorea.dto.JkResult;
import com.mrpark.dev.wooriportal.jobinfo.jobkorea.service.JobKoreaCrawlerService;
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
public class JobKoreaController {

    private final JobKoreaCrawlerService crawlerService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/jobinfo/jobkorea")
    public String index() { return "jobinfo/jobkorea/index"; }

    @PostMapping("/jobinfo/jobkorea/api/search")
    @ResponseBody
    public ResponseEntity<JkResult> search(@RequestBody JkRequest req) {
        return ResponseEntity.ok(crawlerService.crawl(req));
    }

    /** SSE 진행률 스트림 */
    @GetMapping(value = "/jobinfo/jobkorea/api/search-all/progress/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter progress(@PathVariable String jobId) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(()   -> emitters.remove(jobId));
        return emitter;
    }

    /** 전체 수집 시작 → jobId 반환 */
    @PostMapping("/jobinfo/jobkorea/api/search-all")
    @ResponseBody
    public ResponseEntity<Map<String, String>> searchAll(@RequestBody JkRequest req,
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

                JkResult result = crawlerService.crawlAllWithProgress(
                        req, maxPages, jobId,
                        (cur, tot, cnt) -> {
                            if (fe == null) return;
                            try {
                                fe.send(SseEmitter.event().name("progress")
                                        .data(Map.of("currentPage", cur, "totalPages", tot, "collectedCount", cnt)));
                            } catch (Exception e) { /* client disconnected / completed */ }
                        }
                );

                if (fe != null && !crawlerService.isCancelled(jobId)) {
                    try { fe.send(SseEmitter.event().name("done").data(result)); fe.complete(); }
                    catch (Exception e) { /* client disconnected / completed */ }
                }
            } catch (Exception e) {
                log.error("잡코리아 crawlAll 오류: {}", e.getMessage());
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
    @DeleteMapping("/jobinfo/jobkorea/api/search-all/{jobId}")
    @ResponseBody
    public ResponseEntity<Void> cancel(@PathVariable String jobId) {
        crawlerService.cancelJob(jobId);
        SseEmitter e = emitters.remove(jobId);
        if (e != null) {
            try { e.complete(); } catch (Exception ex) { /* ignore */ }
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/jobinfo/jobkorea/api/build-url")
    @ResponseBody
    public ResponseEntity<String> buildUrl(@RequestBody JkRequest req) {
        return ResponseEntity.ok(crawlerService.buildSearchUrl(req));
    }
}
