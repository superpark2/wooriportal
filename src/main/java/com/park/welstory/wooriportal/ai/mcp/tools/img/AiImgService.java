package com.park.welstory.wooriportal.ai.mcp.tools.img;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.park.welstory.wooriportal.ai.AiHandler;
import com.park.welstory.wooriportal.ai.AiSessionStore;
import com.park.welstory.wooriportal.ai.lora.LoraConfig;
import com.park.welstory.wooriportal.ai.lora.LoraWorkflowInjector;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │                        AiImgService                                  │
 * │  ComfyUI를 통한 이미지 생성·편집 전담 서비스.                          │
 * │                                                                      │
 * │  AiService(채팅)와 완전히 분리되어 있으며,                              │
 * │  세션 이미지 상태는 AiSessionStore를 통해 공유한다.                    │
 * │                                                                      │
 * │  워크플로우: painter.json (v2 고정)                                   │
 * │  ┌─────────────────────────────────────────────────────────────┐    │
 * │  │  진입점: generate(...)                                       │    │
 * │  │    ├─ T2I  (images 0장) → 301/302/303 경로                  │    │
 * │  │    ├─ I2I  (images 1장) → 1_image 편집, LanPaint 비활성화   │    │
 * │  │    ├─ 2img (images 2장) → 2_image 합성/편집 모드             │    │
 * │  │    └─ 3img (images 3장) → 3_image 합성/편집 모드             │    │
 * │  └─────────────────────────────────────────────────────────────┘    │
 * └──────────────────────────────────────────────────────────────────────┘
 */
@Service
@RequiredArgsConstructor
public class AiImgService {

    private final AiHandler aiHandler;
    private final AiSessionStore sessionStore;

    @Value("${comfy.url}")
    private String COMFY_URL;

    private final String COMFY_IMAGE_DIR = System.getProperty("user.dir") + "/ai/image/AIgen";
    private final ObjectMapper         mapper       = new ObjectMapper();
    private final LoraWorkflowInjector loraInjector = new LoraWorkflowInjector(mapper);

    private static final String WF_PAINTER = "workflows/painter.json";

    // ── painter.json v2 고정 노드 ID ─────────────────────────────────────
    private static final String   PROMPT_NODE      = "206";
    private static final String   PAINTER_NODE     = "207";
    private static final String   KSAMPLER_NODE    = "208";
    private static final String[] LOAD_IMAGE_NODES = {"203", "204", "227"};

    @PostConstruct
    public void init() {
        try {
            var p = Paths.get(COMFY_IMAGE_DIR);
            if (!Files.exists(p)) Files.createDirectories(p);
        } catch (Exception e) {
            System.err.println("[AiImgService] 이미지 폴더 생성 실패: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  진입점
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 이미지 생성·편집 처리.
     *
     * @param prompt       영어 변환된 프롬프트 (LLM이 분석한 결과)
     * @param stage2Prompt LanPaint 2차 스테이지 프롬프트 (2장 이상 편집 시)
     * @param images       사용할 이미지 Base64 목록 (0~3장)
     * @param imageOrder   이미지 슬롯 순서 (0-based, LLM이 지정)
     * @param originalMsg  채팅 메모리용 원본 메시지
     * @param sessionId    세션 ID
     * @param ctx          SSE 세션 컨텍스트
     * @param emitter      SSE 에미터
     */
    public void generate(String prompt, String stage2Prompt, List<String> images, List<Integer> imageOrder,
                         String originalMsg, String sessionId,
                         AiHandler.SessionContext ctx, SseEmitter emitter) {
        handlePainterGen(prompt, stage2Prompt, images, imageOrder, originalMsg, sessionId, ctx, emitter);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  painter.json v2 워크플로우 처리
    // ══════════════════════════════════════════════════════════════════════════

    private void handlePainterGen(String prompt, String stage2Prompt, List<String> images, List<Integer> imageOrder,
                                  String originalMsg, String sessionId,
                                  AiHandler.SessionContext ctx, SseEmitter emitter) {
        try {
            if (ctx.isCancelled()) return;

            List<String> orderedImages = applyImageOrder(images, imageOrder);
            int imageCount = orderedImages.size(); // 0~3

            Optional<LoraConfig> lora = LoraConfig.detect(originalMsg);
            String finalPrompt = lora.map(l -> l.buildPrompt(prompt)).orElse(prompt);

            if (imageCount == 0) {
                sendToken(emitter, "🎨 이미지 생성 중이에요 (약 1~2분)...\n");
            } else {
                sendToken(emitter, "🖌️ 이미지 편집 중이에요 (" + imageCount + "장 참조) (약 1~2분)...\n");
            }

            // ── 이미지 업로드 (최대 3장) ─────────────────────────────────────
            List<String> uploadedNames = new ArrayList<>();
            for (String b64 : orderedImages) {
                if (ctx.isCancelled()) return;
                String name = uploadImageToComfy(b64);
                if (name == null) { finishWithError(emitter, "이미지 업로드에 실패했어요."); return; }
                uploadedNames.add(name);
            }

            // ── 워크플로우 로드 ───────────────────────────────────────────────
            ObjectNode wf = loadWorkflow(WF_PAINTER);
            if (wf == null) { finishWithError(emitter, "워크플로우를 불러올 수 없어요."); return; }

            // ── 프롬프트 주입 ────────────────────────────────────────────────
            if (finalPrompt != null && !finalPrompt.isBlank())
                ((ObjectNode) wf.path(PROMPT_NODE).path("inputs")).put("value", finalPrompt);

            // ── PainterFluxImageEdit 노드 설정 ──────────────────────────────
            ObjectNode painterInputs = (ObjectNode) wf.path(PAINTER_NODE).path("inputs");
            String modeValue = switch (imageCount) {
                case 2  -> "2_image";
                case 3  -> "3_image";
                default -> "1_image";
            };
            painterInputs.put("mode", modeValue);
            painterInputs.put("batch_size", 1);
            ArrayNode wv = (ArrayNode) wf.path(PAINTER_NODE).get("widgets_values");
            if (wv != null && wv.size() > 1) {
                wv.set(1, new TextNode(modeValue));
            }

            // ── LoadImage 노드에 업로드 파일명 주입 ──────────────────────────
            String[] imageInputKeys = {"image1", "image2", "image3"};
            for (int i = 0; i < uploadedNames.size(); i++) {
                ObjectNode li = (ObjectNode) wf.path(LOAD_IMAGE_NODES[i]).path("inputs");
                if (!li.isMissingNode()) li.put("image", uploadedNames.get(i));
            }
            for (int i = uploadedNames.size(); i < LOAD_IMAGE_NODES.length; i++) {
                wf.remove(LOAD_IMAGE_NODES[i]);
                painterInputs.remove(imageInputKeys[i]);
            }

            // ── T2I 모드 (이미지 0장) ────────────────────────────────────────
            if (imageCount == 0) {
                ((ObjectNode) wf.path("301").path("inputs"))
                        .put("text", finalPrompt != null ? finalPrompt : "");

                ObjectNode kInputs = (ObjectNode) wf.path(KSAMPLER_NODE).path("inputs");
                kInputs.set("positive",     mapper.createArrayNode().add("301").add(0));
                kInputs.set("negative",     mapper.createArrayNode().add("302").add(0));
                kInputs.set("latent_image", mapper.createArrayNode().add("303").add(0));

                wf.remove(PAINTER_NODE); // "207"
                wf.remove("205");        // GetImageSize
                wf.remove("218");        // ImageScaleToTotalPixels
                wf.remove("219");        // VAEEncode
                wf.remove("220");        // ReferenceLatent
                wf.remove("221");        // FluxGuidance

                ObjectNode lp = (ObjectNode) wf.path("223").path("inputs");
                if (!lp.isMissingNode()) {
                    lp.set("positive", mapper.createArrayNode().add("301").add(0));
                    lp.set("negative", mapper.createArrayNode().add("302").add(0));
                }
            }

            // ── KSampler 시드 랜덤화 ─────────────────────────────────────────
            ((ObjectNode) wf.path(KSAMPLER_NODE).path("inputs"))
                    .put("seed", (long)(Math.random() * Long.MAX_VALUE));

            // ── 2단계(LanPaint) 동적 처리 ────────────────────────────────────
            if (imageCount > 0) {
                if (!wf.path("223").isMissingNode()) {
                    ((ObjectNode) wf.path("223").path("inputs"))
                            .put("seed", (long)(Math.random() * Long.MAX_VALUE));
                }

                if (imageCount >= 2 && finalPrompt != null && !finalPrompt.isBlank()) {
                    String s2 = (stage2Prompt != null && !stage2Prompt.isBlank())
                            ? stage2Prompt : finalPrompt;
                    ObjectNode clip216 = (ObjectNode) wf.path("216").path("inputs");
                    if (!clip216.isMissingNode()) {
                        clip216.put("text", s2);
                    } else {
                        System.err.println("[AiImgService] 노드 '216' 없음 — stage2Prompt 주입 실패");
                    }
                }

                if (imageCount < 2) {
                    // 1장 단독 편집: LanPaint 2차 스테이지 전체 비활성화
                    for (String nodeId : new String[]{"216","217","218","219","220","221","222","223","224","225"}) {
                        if (!wf.path(nodeId).isMissingNode()) wf.remove(nodeId);
                    }
                    ObjectNode save226 = (ObjectNode) wf.path("226").path("inputs");
                    if (!save226.isMissingNode()) {
                        save226.set("images", mapper.createArrayNode().add("209").add(0));
                    }
                    System.out.println("[AiImgService] 단독 편집: LanPaint 2차 스테이지 비활성화, 1차 결과 직출력");
                } else {
                    // 2장 이상: "218"의 레퍼런스를 image2("204")로 고정
                    ObjectNode in218 = (ObjectNode) wf.path("218").path("inputs");
                    if (!in218.isMissingNode()) {
                        in218.set("image", mapper.createArrayNode().add("204").add(0));
                    }
                }
            }

            // ── LoRA 주입 ─────────────────────────────────────────────────────
            loraInjector.injectPainterV2(wf, lora);

            // ── 제출 ─────────────────────────────────────────────────────────
            if (ctx.isCancelled()) return;
            String promptId = submitComfyPrompt(wf);
            if (promptId == null) { finishWithError(emitter, "이미지 서버에 연결할 수 없어요."); return; }

            ctx.setComfyPromptId(promptId);
            String imageFileName = pollComfyResult(promptId, 300, ctx);

            if (ctx.isCancelled()) {
                cancelComfyPrompt(promptId);
                return;
            }

            deleteComfyHistory(promptId);

            if (imageFileName == null) {
                finishWithError(emitter, "이미지 생성 시간이 초과됐어요.");
                return;
            }

            String saved = downloadAndSaveComfyImage(imageFileName);
            if (saved == null) { finishWithError(emitter, "이미지를 저장하는 데 실패했어요."); return; }

            // ── 세션 이미지 갱신 ─────────────────────────────────────────────
            String cachedB64 = loadImageFileAsBase64(saved);
            if (cachedB64 != null) {
                sessionStore.updateAfterGeneration(sessionId, saved, cachedB64);
            } else {
                sessionStore.putLastImageFile(sessionId, saved);
                System.err.println("[AiImgService] ⚠ b64 캐싱 실패 → 파일명만 저장, allImages 불일치 발생: " + saved
                        + " | 다음 요청 시 route()의 resolveSessionImage fallback으로 자동 복구됩니다.");
            }

            // ── SSE 완료 이벤트 전송 ─────────────────────────────────────────
            String imgUrl  = "/ai/image/AIgen/" + saved;
            String comment = imageCount == 0 ? "이미지를 생성했어요!" : "이미지 편집이 완료됐어요!";

            emitter.send(SseEmitter.event().name("done")
                    .data("{\"fullContent\":" + mapper.writeValueAsString("[AI_IMAGE:" + imgUrl + "]")
                            + ",\"imageUrl\":"  + mapper.writeValueAsString(imgUrl)
                            + ",\"comment\":"   + mapper.writeValueAsString(comment) + "}"));
            emitter.complete();
            ctx.complete();

        } catch (Exception e) {
            System.err.println("[AiImgService] Painter 이미지 처리 오류: " + e.getMessage());
            if (!ctx.isCancelled()) sendError(emitter, "이미지 처리 중 오류가 발생했습니다.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  세션 이미지 유틸 (AiService에서 호출 가능하도록 public)
    // ══════════════════════════════════════════════════════════════════════════

    /** 세션의 이미지 B64 캐시 반환. 파일명만 있으면 파일에서 로드해 캐싱. */
    public String resolveSessionImage(String sessionId) {
        Optional<String> cached = sessionStore.getLastImageB64(sessionId);
        if (cached.isPresent()) return cached.get();

        return sessionStore.getLastImageFile(sessionId).map(fileName -> {
            String b64 = loadImageFileAsBase64(fileName);
            if (b64 != null) sessionStore.putLastImageB64(sessionId, b64);
            return b64;
        }).orElse(null);
    }

    /** 로컬 저장된 이미지 파일을 Base64로 변환 */
    public String loadImageFileAsBase64(String savedFileName) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(COMFY_IMAGE_DIR, savedFileName));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ComfyUI 통신
    // ══════════════════════════════════════════════════════════════════════════

    private void cancelComfyPrompt(String promptId) {
        if (promptId == null) return;
        try {
            ObjectNode body = mapper.createObjectNode();
            body.set("delete", mapper.createArrayNode().add(promptId));
            HttpURLConnection conn = openJsonPost(COMFY_URL + "/queue");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            try (OutputStream os = conn.getOutputStream()) { os.write(mapper.writeValueAsBytes(body)); }
            conn.getResponseCode();
            System.out.println("[AiImgService] cancelComfyPrompt: " + promptId);
        } catch (Exception e) {
            System.err.println("[AiImgService] cancelComfyPrompt 실패: " + e.getMessage());
        }
    }

    private void deleteComfyHistory(String promptId) {
        if (promptId == null) return;
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("delete", promptId);
            HttpURLConnection conn = openJsonPost(COMFY_URL + "/history");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            try (OutputStream os = conn.getOutputStream()) { os.write(mapper.writeValueAsBytes(body)); }
            conn.getResponseCode();
        } catch (Exception e) {
            System.err.println("[AiImgService] deleteComfyHistory 실패: " + e.getMessage());
        }
    }

    private String uploadImageToComfy(String base64Data) {
        try {
            String pureBase64 = base64Data.contains(",")
                    ? base64Data.substring(base64Data.indexOf(',') + 1) : base64Data;
            byte[] imageBytes = Base64.getDecoder().decode(pureBase64);
            String boundary   = "----WooriBoundary" + UUID.randomUUID().toString().replace("-", "");
            String uploadName = "upload_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
            HttpURLConnection conn = (HttpURLConnection)
                    URI.create(COMFY_URL + "/upload/image").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(600_000);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"type\"\r\n\r\ninput\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\""
                        + uploadName + "\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(imageBytes);
                os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            if (conn.getResponseCode() != 200) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            return mapper.readTree(sb.toString()).path("name").asText(null);
        } catch (Exception e) {
            System.err.println("[AiImgService] ComfyUI 이미지 업로드 오류: " + e.getMessage());
            return null;
        }
    }

    private String submitComfyPrompt(ObjectNode workflow) throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.set("prompt", workflow);
        payload.put("client_id", "woori_" + UUID.randomUUID().toString().substring(0, 8));
        HttpURLConnection conn = openJsonPost(COMFY_URL + "/prompt");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(360_000);
        try (OutputStream os = conn.getOutputStream()) { os.write(mapper.writeValueAsBytes(payload)); }
        if (conn.getResponseCode() != 200) return null;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return mapper.readTree(sb.toString()).path("prompt_id").asText(null);
    }

    private String pollComfyResult(String promptId, int timeoutSeconds, AiHandler.SessionContext ctx) {
        long deadline = System.currentTimeMillis() + (long) timeoutSeconds * 1000;
        while (System.currentTimeMillis() < deadline) {
            if (ctx != null && ctx.isCancelled()) {
                System.out.println("[AiImgService] pollComfyResult 취소 감지.");
                return null;
            }
            try {
                Thread.sleep(2000);
                if (ctx != null && ctx.isCancelled()) return null;

                HttpURLConnection conn = (HttpURLConnection)
                        URI.create(COMFY_URL + "/history/" + promptId).toURL().openConnection();
                conn.setConnectTimeout(5_000);
                conn.setReadTimeout(10_000);
                if (conn.getResponseCode() != 200) continue;
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                JsonNode root  = mapper.readTree(sb.toString());
                JsonNode entry = root.path(promptId);
                if (entry.isMissingNode()) continue;
                Iterator<JsonNode> it = entry.path("outputs").elements();
                while (it.hasNext()) {
                    JsonNode imgs = it.next().path("images");
                    if (imgs.isArray() && !imgs.isEmpty()) {
                        JsonNode img       = imgs.get(0);
                        String   filename  = img.path("filename").asText(null);
                        String   type      = img.path("type").asText("output");
                        String   subfolder = img.path("subfolder").asText("");
                        if (filename == null) continue;
                        return filename + "::" + type + "::" + subfolder;
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                System.err.println("[AiImgService] ComfyUI 폴링 오류: " + e.getMessage());
            }
        }
        return null;
    }

    private String downloadAndSaveComfyImage(String comfyFileInfo) {
        try {
            String[] parts         = comfyFileInfo.split("::", -1);
            String   comfyFileName = parts[0];
            String   fileType      = parts.length > 1 ? parts[1] : "output";
            String   subfolder     = parts.length > 2 ? parts[2] : "";
            String downloadUrl = COMFY_URL + "/view?filename="
                    + URLEncoder.encode(comfyFileName, StandardCharsets.UTF_8)
                    + "&type=" + URLEncoder.encode(fileType, StandardCharsets.UTF_8)
                    + "&subfolder=" + URLEncoder.encode(subfolder, StandardCharsets.UTF_8);
            HttpURLConnection conn = (HttpURLConnection) URI.create(downloadUrl).toURL().openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(120_000);
            if (conn.getResponseCode() != 200) return null;
            byte[] imageBytes = conn.getInputStream().readAllBytes();
            String ext = "png";
            String ct  = conn.getContentType();
            if      (ct != null && ct.contains("jpeg")) ext = "jpg";
            else if (ct != null && ct.contains("webp")) ext = "webp";
            String savedName = "gen_" + UUID.randomUUID().toString().substring(0, 10) + "." + ext;
            Files.write(Paths.get(COMFY_IMAGE_DIR, savedName), imageBytes);
            return savedName;
        } catch (Exception e) {
            System.err.println("[AiImgService] ComfyUI 이미지 다운로드 실패: " + e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  워크플로우 유틸
    // ══════════════════════════════════════════════════════════════════════════

    private ObjectNode loadWorkflow(String resourcePath) {
        try (var is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) { System.err.println("[AiImgService] 워크플로우 파일 없음: " + resourcePath); return null; }
            return (ObjectNode) mapper.readTree(is);
        } catch (Exception e) {
            System.err.println("[AiImgService] 워크플로우 로드 오류 [" + resourcePath + "]: " + e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  이미지 유틸
    // ══════════════════════════════════════════════════════════════════════════

    private List<String> applyImageOrder(List<String> images, List<Integer> imageOrder) {
        if (images.isEmpty() || imageOrder.isEmpty()) return new ArrayList<>(images);
        List<String> result = new ArrayList<>();
        Set<Integer> used   = new LinkedHashSet<>();
        for (int idx : imageOrder) {
            if (idx >= 0 && idx < images.size() && !used.contains(idx)) {
                result.add(images.get(idx));
                used.add(idx);
            }
        }
        // fallback 제거 — imageOrder에 명시되지 않은 이미지는 포함하지 않음
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SSE / HTTP 유틸
    // ══════════════════════════════════════════════════════════════════════════

    private HttpURLConnection openJsonPost(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        return conn;
    }

    private void sendToken(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().name("token")
                    .data("{\"token\":" + mapper.writeValueAsString(text) + "}"));
        } catch (Exception ignored) {}
    }

    private void sendError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data("{\"message\":\"" + msg + "\"}"));
            emitter.complete();
        } catch (Exception ignored) {}
    }

    private void finishWithError(SseEmitter emitter, String errMsg) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data("{\"message\":" + mapper.writeValueAsString(errMsg) + "}"));
            emitter.complete();
        } catch (Exception ignored) {}
    }
}