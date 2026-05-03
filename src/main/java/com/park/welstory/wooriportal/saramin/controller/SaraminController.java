package com.park.welstory.wooriportal.saramin.controller;

import com.park.welstory.wooriportal.saramin.dto.SearchRequestDto;
import com.park.welstory.wooriportal.saramin.dto.SearchResultDto;
import com.park.welstory.wooriportal.saramin.service.SaraminCrawlerService;
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
public class SaraminController {

    private final SaraminCrawlerService crawlerService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/saramin")
    public String index() { return "saramin/index"; }

    @PostMapping("/saramin/api/search")
    @ResponseBody
    public ResponseEntity<SearchResultDto> search(@RequestBody SearchRequestDto req) {
        return ResponseEntity.ok(crawlerService.crawl(req));
    }

    /** SSE 진행률 스트림 */
    @GetMapping(value = "/saramin/api/search-all/progress/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter progress(@PathVariable String jobId) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(()   -> emitters.remove(jobId));
        return emitter;
    }

    /** 전체 수집 시작 → jobId 반환 */
    @PostMapping("/saramin/api/search-all")
    @ResponseBody
    public ResponseEntity<Map<String, String>> searchAll(@RequestBody SearchRequestDto req,
                                                         @RequestParam(defaultValue = "0") int maxPages) {
        String jobId = UUID.randomUUID().toString();

        executor.submit(() -> {
            SseEmitter emitter = null;
            try {
                // emitter 연결 대기 (최대 2초)
                for (int i = 0; i < 20; i++) {
                    emitter = emitters.get(jobId);
                    if (emitter != null) break;
                    Thread.sleep(100);
                }
                final SseEmitter fe = emitter;

                SearchResultDto result = crawlerService.crawlAllWithProgress(
                        req, maxPages, jobId,
                        (cur, tot, cnt) -> {
                            if (fe == null) return;
                            try {
                                fe.send(SseEmitter.event().name("progress")
                                        .data(Map.of("currentPage", cur, "totalPages", tot, "collectedCount", cnt)));
                            } catch (IOException e) { /* client disconnected */ }
                        }
                );

                if (fe != null) {
                    fe.send(SseEmitter.event().name("done").data(result));
                    fe.complete();
                }
            } catch (Exception e) {
                log.error("crawlAll 오류: {}", e.getMessage(), e);
                if (emitter != null) {
                    try { emitter.send(SseEmitter.event().name("error").data(e.getMessage())); emitter.complete(); }
                    catch (IOException ex) { /* ignore */ }
                }
            }
        });

        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    /** 취소 */
    @DeleteMapping("/saramin/api/search-all/{jobId}")
    @ResponseBody
    public ResponseEntity<Void> cancel(@PathVariable String jobId) {
        crawlerService.cancelJob(jobId);
        SseEmitter e = emitters.get(jobId);
        if (e != null) {
            try { e.send(SseEmitter.event().name("cancelled").data("cancelled")); e.complete(); }
            catch (IOException ex) { /* ignore */ }
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/saramin/api/build-url")
    @ResponseBody
    public ResponseEntity<String> buildUrl(@RequestBody SearchRequestDto req) {
        return ResponseEntity.ok(crawlerService.buildSearchUrl(req));
    }

    @GetMapping("/api/debug-html")
    @ResponseBody
    public ResponseEntity<String> debugHtml() {
        return ResponseEntity.ok(crawlerService.fetchRawHtml());
    }
}