package com.mrpark.dev.wooriportal.ai.mcp.tools.excel;

import com.mrpark.dev.wooriportal.ai.AiHandler;
import com.mrpark.dev.wooriportal.ai.mcp.McpTool;
import com.mrpark.dev.wooriportal.ai.mcp.dto.ToolDefinitionDTO;
import com.mrpark.dev.wooriportal.ai.mcp.tools.excel.dto.ExcelToolArgumentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * MCP 도구: 엑셀 AI 처리.
 *
 * 엑셀 파일은 세션 컨텍스트에 별도로 보관되어 있으므로
 * argumentsJson 에는 처리 지시(prompt)만 담긴다.
 * 파일은 ctx 를 통해 가져온다.
 */
@Component
@RequiredArgsConstructor
public class ExcelTool implements McpTool {

    private final ExcelAiService excelAiService;

    @Override
    public String getName() {
        return "excel_process";
    }

    @Override
    public ToolDefinitionDTO getDefinition() {
        return ToolDefinitionDTO.builder()
                .function(ToolDefinitionDTO.FunctionDefinition.builder()
                        .name(getName())
                        .description("""
                                업로드된 엑셀 파일을 AI로 처리한다.
                                데이터 분석, 정렬, 필터링, 수식 생성 등
                                엑셀 관련 작업 요청 시 사용한다.
                                """)
                        .parameters(ToolDefinitionDTO.ParameterSchema.builder()
                                .properties(Map.of(
                                        "prompt", ToolDefinitionDTO.PropertySchema.builder()
                                                .type("string")
                                                .description("엑셀 처리 요청 내용 (예: '3열 기준 내림차순 정렬')")
                                                .build()
                                ))
                                .required(List.of("prompt"))
                                .build())
                        .build())
                .build();
    }

    @Override
    public void execute(String argumentsJson, String sessionId,
                        AiHandler.SessionContext ctx, SseEmitter emitter) {
        try {
            ExcelToolArgumentDTO args = MAPPER.readValue(argumentsJson, ExcelToolArgumentDTO.class);

            MultipartFile file = ctx.getPendingExcelFile();
            if (file == null) {
                sendError(emitter, "처리할 엑셀 파일이 없습니다. 파일을 먼저 업로드해주세요.");
                return;
            }

            System.out.println("[ExcelTool] 처리 시작. prompt=" + args.getPrompt());
            excelAiService.processExcel(file, args.getPrompt(), ctx);

        } catch (Exception e) {
            System.err.println("[ExcelTool] 오류: " + e.getMessage());
            sendError(emitter, "엑셀 처리 중 오류가 발생했습니다.");
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data("{\"message\":\"" + message + "\"}"));
            emitter.complete();
        } catch (Exception ignored) {}
    }
}
