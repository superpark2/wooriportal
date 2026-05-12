package com.park.welstory.wooriportal.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 채팅/이미지 서비스가 공유하는 세션 상태 저장소.
 *
 * AiService(채팅)와 AiImgService(이미지) 양쪽에서 주입받아 사용한다.
 * 세션별 이미지 목록·마지막 이미지 파일명·Base64 캐시를 관리한다.
 */
@Component
public class AiSessionStore {

    /** 세션별 전체 첨부 이미지 목록 (최대 3장) */
    private final Map<String, List<String>> sessionAllImages     = new ConcurrentHashMap<>();

    /** 세션별 마지막으로 저장된 이미지 파일명 (ComfyUI 저장 결과) */
    private final Map<String, String>       sessionLastImageFile = new ConcurrentHashMap<>();

    /** 세션별 마지막 이미지 Base64 캐시 */
    private final Map<String, String>       sessionLastImageB64  = new ConcurrentHashMap<>();

    /** 세션별 전체 이미지 파일 경로 목록 **/
    private final Map<String, List<String>> sessionImagePaths = new ConcurrentHashMap<>();


    // ── 이미지 목록 ──────────────────────────────────────────────────

    public List<String> getAllImages(String sessionId) {
        return sessionAllImages.getOrDefault(sessionId, List.of());
    }

    public void putAllImages(String sessionId, List<String> images) {
        sessionAllImages.put(sessionId, new ArrayList<>(images));
    }

    public void removeAllImages(String sessionId) {
        sessionAllImages.remove(sessionId);
    }

    // ── 마지막 이미지 파일명 ─────────────────────────────────────────

    public Optional<String> getLastImageFile(String sessionId) {
        return Optional.ofNullable(sessionLastImageFile.get(sessionId));
    }

    public void putLastImageFile(String sessionId, String fileName) {
        sessionLastImageFile.put(sessionId, fileName);
    }

    public void removeLastImageFile(String sessionId) {
        sessionLastImageFile.remove(sessionId);
    }

    // ── 마지막 이미지 Base64 캐시 ────────────────────────────────────

    public Optional<String> getLastImageB64(String sessionId) {
        return Optional.ofNullable(sessionLastImageB64.get(sessionId));
    }

    public void putLastImageB64(String sessionId, String b64) {
        sessionLastImageB64.put(sessionId, b64);
    }

    public void removeLastImageB64(String sessionId) {
        sessionLastImageB64.remove(sessionId);
    }

    // ── 원자적 갱신 ─────────────────────────────────────────────────

    /**
     * 이미지 생성 완료 시 세 가지 상태를 원자적으로 교체.
     * 개별 put()을 순차 호출하면 중간 상태가 다른 스레드에 노출될 수 있으므로
     * generate() 완료 후 반드시 이 메서드 하나로 통일한다.
     */
    public void updateAfterGeneration(String sessionId, String fileName, String b64) {
        sessionLastImageFile.put(sessionId, fileName);
        sessionLastImageB64.put(sessionId, b64);
        // allImages는 건드리지 않음 — 첨부 이미지 상태 유지 (ImageTool.resolveImages()가 조합)
        sessionImagePaths.put(sessionId, new ArrayList<>(List.of(fileName)));
    }

    public List<String> getImagePaths(String sessionId) {
        return sessionImagePaths.getOrDefault(sessionId, List.of());
    }

    /**
     * 단독 편집(이전 생성 이미지 불필요) 시 생성 이미지 상태만 원자적으로 초기화.
     * allImages는 신규 첨부로 이미 교체된 상태이므로 건드리지 않는다.
     */
    public void clearGeneratedImages(String sessionId) {
        sessionLastImageFile.remove(sessionId);
        sessionLastImageB64.remove(sessionId);
    }

    // ── 세션 전체 초기화 ─────────────────────────────────────────────

    public void clearSession(String sessionId) {
        sessionAllImages.remove(sessionId);
        sessionLastImageFile.remove(sessionId);
        sessionLastImageB64.remove(sessionId);
        sessionImagePaths.remove(sessionId);
        sessionLastUsedImages.remove(sessionId);
    }


    // ── 세션 이미지 다시생성용 ─────────────────────────────────────────────

    private final Map<String, List<String>> sessionLastUsedImages = new ConcurrentHashMap<>();

    public void putLastUsedImages(String sessionId, List<String> images) {
        sessionLastUsedImages.put(sessionId, new ArrayList<>(images));
    }

    public List<String> getLastUsedImages(String sessionId) {
        return sessionLastUsedImages.getOrDefault(sessionId, List.of());
    }
}