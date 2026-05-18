package com.mrpark.dev.wooriportal.aicoach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrpark.dev.wooriportal.aicoach.dto.AiCoachRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Ollama API 호출 + 면접 AI(질문생성/피드백) + 자소서 AI(맞춤법/피드백/이미지) 통합 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCoachAiService {

    @Value("${ollama.api.url}")  private String ollamaApiUrl;
    @Value("${ollama.model}")    private String ollamaModel;

    @Value("${ollama.options.num-ctx}")
    private int numCtx;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> TYPE_GUIDE = Map.of(
        "행동", """
            - 지원자의 가치관과 행동 방식을 파악하는 질문
            - 실제 경험이 없어도 답할 수 있는 상황 제시형 질문
            - 예: "팀 내 의견 충돌이 생겼을 때 어떻게 대처하실 건가요?"
            - 예: "업무 중 예상치 못한 문제가 발생하면 어떻게 해결할 것인가요?" """,
        "기술", """
            - 직무에 필요한 기술과 지식을 묻는 질문
            - 실무 경험 없이도 학습 수준과 이해도로 답할 수 있는 질문
            - 예: "직무에서 가장 중요하다고 생각하는 역량은 무엇인가요?"
            - 예: "회사에서 직무로 일하기 위해 어떤 준비를 해왔나요?" """,
        "인성", """
            - 지원자의 가치관, 성격, 지원 동기를 파악하는 질문
            - 회사의 기업문화와 비전에 맞는 인재인지 평가
            - 예: "회사에 지원한 이유는 무엇인가요?"
            - 예: "10년 후 본인의 모습은 어떨 것 같나요?" """
    );

    // ================================================================
    // 면접 AI
    // ================================================================

    public List<String> generateQuestions(AiCoachRequestDto.GenerateQuestionsRequest req) {
        String guide = TYPE_GUIDE.getOrDefault(req.getInterviewType(), TYPE_GUIDE.get("행동"));
        String prompt = """
            당신은 %s 기업의 %s 직무 채용 면접관입니다.
            지원자는 아직 해당 직무 경험이 없는 신입 지원자입니다.
            매우 엄격한 자세로 임해주십시오.

            [면접 유형: %s 면접]
            %s

            [중요 규칙]
            - 반드시 신입 지원자도 답할 수 있는 질문으로 생성
            - "~했던 경험을 말해주세요" 같은 경험 기반 질문 절대 금지
            - 대신 "~한다면 어떻게 하시겠습니까?" 또는 "~에 대해 어떻게 생각하십니까?" 형식 사용
            - 총 %d개 질문 생성
            - 반드시 아래 JSON 배열 형식으로만 답변 (다른 텍스트 절대 금지)

            ["질문1", "질문2", "질문3"]
            """.formatted(req.getCompany(), req.getJob(),
                          req.getInterviewType(), guide,
                          req.getQuestionCount() != null ? req.getQuestionCount() : 5);

        return parseJson(generate(prompt), new TypeReference<List<String>>() {});
    }

    public Map<String, Object> generateFeedback(AiCoachRequestDto.GenerateFeedbackRequest req) {
        String prompt = """
            당신은 %s 기업의 %s 직무 전문 면접관입니다.
            매우 엄격한 자세로 임해주십시오.
            질문: %s
            지원자 답변: %s

            위 답변을 분석해서 반드시 아래 JSON 형식으로만 답변하세요 (다른 텍스트 절대 금지):
            {
              "good": "잘한 점 (2~5문장)",
              "improve": "개선할 점 (2~5문장)",
              "score": 종합점수(1-100 숫자만),
              "star_score": STAR구조활용도(1-100 숫자만),
              "relevance_score": 직무연관성(1-100 숫자만),
              "detail_score": 구체성(1-100 숫자만),
              "hint": "모범 답안 첫 문장 힌트 (30자 내외, 저는 또는 당시 로 시작)",
              "full_answer": "모범 답안 전체 (200자 내외)"
            }
            """.formatted(req.getCompany(), req.getJob(), req.getQuestion(), req.getAnswer());

        return parseJson(generate(prompt), new TypeReference<Map<String, Object>>() {});
    }

    // ================================================================
    // 자소서 AI
    // ================================================================

    public Map<String, Object> spellCheck(AiCoachRequestDto.SpellCheckRequest req) {
        if (req.getContent() == null || req.getContent().isBlank())
            throw new IllegalArgumentException("내용을 입력해주세요.");

        String prompt = """
            당신은 한국어 맞춤법 및 문장 교정 전문가입니다.
            아래 자기소개서 텍스트의 맞춤법, 띄어쓰기, 문장 오류를 검사해주세요.

            [자기소개서]
            %s

            반드시 아래 JSON 형식으로만 답변하세요 (다른 텍스트 절대 금지):
            {"errors":[{"original":"틀린단어","corrected":"올바른단어","reason":"교정이유"}],"corrected_text":"전체교정된텍스트","error_count":오류개수}
            """.formatted(req.getContent());

        return parseJson(generate(prompt), new TypeReference<Map<String, Object>>() {});
    }

    public Map<String, Object> resumeFeedback(AiCoachRequestDto.ResumeFeedbackRequest req) {
        if (req.getContent() == null || req.getContent().isBlank())
            throw new IllegalArgumentException("내용을 입력해주세요.");

        String prompt = """
            당신은 대기업 채용 전문가이자 자기소개서 컨설턴트입니다.
            매우 엄격한 자세로 임해주십시오.

            [지원 회사] %s
            [지원 직무] %s
            [자기소개서]
            %s

            반드시 아래 JSON 형식으로만 답변하세요 (다른 텍스트 절대 금지):
            {"total_score":종합점수(1-100),"scores":{"clarity":명확성(1-100),"specificity":구체성(1-100),"motivation":지원동기(1-100),"fit":직무적합성(1-100)},"strengths":["강점1","강점2","강점3"],"improvements":["개선점1","개선점2","개선점3"],"rewrite_suggestion":"개선예시(200자내외)","overall_comment":"총평(100자내외)"}
            """.formatted(
                req.getCompany() != null ? req.getCompany() : "미입력",
                req.getJob()     != null ? req.getJob()     : "미입력",
                req.getContent());

        return parseJson(generate(prompt), new TypeReference<Map<String, Object>>() {});
    }

    public String extractTextFromImage(AiCoachRequestDto.ExtractImageRequest req) {
        if (req.getImage() == null || req.getImage().isBlank())
            throw new IllegalArgumentException("이미지를 업로드해주세요.");

        String cleanBase64 = req.getImage().replaceAll("^data:image/\\w+;base64,", "");
        String prompt = "아래 이미지에서 텍스트를 추출해주세요. 다른 설명 없이 텍스트만 출력하세요.";

        Map<String, Object> body = Map.of(
            "model",   ollamaModel,
            "prompt",  prompt,
            "images",  List.of(cleanBase64),
            "stream",  false,
            "options", Map.of("num_ctx", numCtx)
        );
        Map<?, ?> result = call(body);
        return (String) result.get("response");
    }

    // ================================================================
    // Ollama 내부 호출
    // ================================================================

    private String generate(String prompt) {
        Map<String, Object> body = Map.of(
            "model",   ollamaModel,
            "prompt",  prompt,
            "stream",  false,
            "options", Map.of("num_ctx", numCtx)
        );
        Map<?, ?> result = call(body);
        return (String) result.get("response");
    }

    @SuppressWarnings("rawtypes")
    private Map<?, ?> call(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> resp = restTemplate.postForEntity(
            ollamaApiUrl + "/generate", new HttpEntity<>(body, headers), Map.class
        );
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null)
            throw new RuntimeException("Ollama API 오류: " + resp.getStatusCode());
        return resp.getBody();
    }

    private <T> T parseJson(String raw, TypeReference<T> ref) {
        try {
            String clean = raw.replace("```json", "").replace("```", "").trim();
            return objectMapper.readValue(clean, ref);
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", raw, e);
            throw new RuntimeException("AI 응답 파싱 실패: " + e.getMessage());
        }
    }
}
