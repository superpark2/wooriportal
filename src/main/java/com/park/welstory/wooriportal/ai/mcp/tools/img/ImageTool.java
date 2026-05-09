package com.park.welstory.wooriportal.ai.mcp.tools.img;

import com.park.welstory.wooriportal.ai.AiHandler;
import com.park.welstory.wooriportal.ai.AiSessionStore;
import com.park.welstory.wooriportal.ai.mcp.McpTool;
import com.park.welstory.wooriportal.ai.mcp.dto.ToolDefinitionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * MCP 도구: 이미지 생성 및 편집.
 *
 * LLM이 intent 를 판단해서 호출:
 *   IMAGE_GEN  → 텍스트 프롬프트로 새 이미지 생성
 *   IMAGE_EDIT → 세션/첨부 이미지를 기반으로 편집
 */
@Component
@RequiredArgsConstructor
public class ImageTool implements McpTool {

    private final AiImgService   aiImgService;
    private final AiSessionStore sessionStore;

    @Override
    public String getName() {
        return "image_action";
    }

    @Override
    public ToolDefinitionDTO getDefinition() {
        return ToolDefinitionDTO.builder()
                .function(ToolDefinitionDTO.FunctionDefinition.builder()
                        .name(getName())
                        .description("""
                                이미지를 생성하거나 편집한다.
                                - 새 이미지가 필요하면 intent=IMAGE_GEN
                                - 첨부/세션 이미지를 수정하려면 intent=IMAGE_EDIT
                                반드시 영어 프롬프트(prompt)를 포함해야 한다.
                                """)
                        .parameters(ToolDefinitionDTO.ParameterSchema.builder()
                                .properties(Map.of(
                                        "prompt", ToolDefinitionDTO.PropertySchema.builder()
                                                .type("string")
                                                .description("이미지 생성/편집에 사용할 영어 프롬프트")
                                                .build(),
                                        "intent", ToolDefinitionDTO.PropertySchema.builder()
                                                .type("string")
                                                .description("IMAGE_GEN: 새 이미지 생성, IMAGE_EDIT: 기존 이미지 편집")
                                                .enumValues(List.of("IMAGE_GEN", "IMAGE_EDIT"))
                                                .build(),
                                        "stage2Prompt", ToolDefinitionDTO.PropertySchema.builder()
                                                .type("string")
                                                .description("2장 이상 합성 시 2차 스테이지 프롬프트 (선택)")
                                                .build(),
                                        "imageOrder", ToolDefinitionDTO.PropertySchema.builder()
                                                .type("array")
                                                .description("편집 시 사용할 이미지 슬롯 순서 (1-based). 예) [1] 또는 [1,2]")
                                                .build()
                                ))
                                .required(List.of("prompt", "intent"))
                                .build())
                        .build())
                .build();
    }

    @Override
    public void execute(String argumentsJson, String sessionId,
                        AiHandler.SessionContext ctx, SseEmitter emitter) {
        try {
            com.park.welstory.wooriportal.ai.mcp.tools.img.dto.ImageToolArgumentDTO args = MAPPER.readValue(argumentsJson, com.park.welstory.wooriportal.ai.mcp.tools.img.dto.ImageToolArgumentDTO.class);

            List<Integer> imageOrder = args.getImageOrder() != null
                    ? args.getImageOrder() : List.of();

            // 1-based → 0-based 변환
            List<Integer> zeroBasedOrder = imageOrder.stream()
                    .filter(o -> o >= 1)
                    .map(o -> o - 1)
                    .toList();

            List<String> images = resolveImages(args.getIntent(), sessionId);

            System.out.println("[ImageTool] intent=" + args.getIntent()
                    + " images=" + images.size() + "장"
                    + " prompt=" + args.getPrompt());

            aiImgService.generate(
                    args.getPrompt(),
                    args.getStage2Prompt(),
                    images,
                    zeroBasedOrder,
                    args.getPrompt(),   // originMsg (메모리용)
                    sessionId,
                    ctx,
                    emitter
            );

        } catch (Exception e) {
            System.err.println("[ImageTool] 실행 오류: " + e.getMessage());
            sendError(emitter, "이미지 처리 중 오류가 발생했습니다.");
        }
    }

    // ── private ───────────────────────────────────────────────────

    private List<String> resolveImages(String intent, String sessionId) {
        if ("IMAGE_GEN".equals(intent)) {
            return List.of();   // 새 생성 → 이미지 불필요
        }

        // IMAGE_EDIT
        // 슬롯 1 = lastGen (이전 생성 이미지)
        // 슬롯 2~ = attached (새 첨부 이미지들)
        // imageOrder가 슬롯 번호를 지정하면 applyImageOrder가 순서대로 뽑아감
        List<String> attached = sessionStore.getAllImages(sessionId);
        String lastGen = aiImgService.resolveSessionImage(sessionId);

        List<String> pool = new java.util.ArrayList<>();
        if (lastGen != null) pool.add(lastGen);  // index 0 = 슬롯 1
        pool.addAll(attached);                    // index 1~ = 슬롯 2~

        return pool;
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data("{\"message\":\"" + message + "\"}"));
            emitter.complete();
        } catch (Exception ignored) {}
    }
}