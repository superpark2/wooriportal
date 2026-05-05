package com.park.welstory.wooriportal.ai.lora;

import java.util.Arrays;
import java.util.Optional;

/**
 * 사용 가능한 LoRA 목록 및 발동 조건 정의.
 *
 * <p>사용자의 한국어 메시지에 keywords 중 하나라도 포함되면
 * 해당 LoRA가 선택됩니다. 매칭 우선순위는 enum 선언 순서입니다.
 * (아래로 내려갈수록 낮은 우선순위)</p>
 */
public enum LoraConfig {

    // ──────────────────────────────────────────────────────────────────────────
    //  우선순위 높음 → 낮음 순으로 선언
    // ──────────────────────────────────────────────────────────────────────────

    Sitcom(
            "90sSitcomKlein9b.safetensors",
            "90s Sitcom",
            1.0,
            "90년대 시트콤", "옛날 시트콤", "고전 시트콤"
    ),

    Ethereal_Gothic_Elegance(
            "EtherialGothicEleganceKlein9b.safetensors",
            "Ethereal Gothic Elegance",
            1.0,
            "우아한 고딕", "에센셜 고딕"
    ),

    ArsNijiStyle(
            "Niji2Klein9b.safetensors",
            "ArsNijiStyle",
            1.0,
            "ArsNiji", "니지 스타일", "니지풍"
    ),

    Crime_Thriller_Movie(
            "2000sCrimeThrillerKlein9b.safetensors",
            "ArsMovieStill, movie still from a gritty, high-contrast 2000s crime thriller movie",
            1.0,
            "옛날 범죄영화", "엣날 스릴러", "고전 범죄영화", "고전 스릴러", "2000년대 범죄영화", "2000년대 스릴러"
    ),

    Noir_Movie_Still(
            "50sNoirKlein9b.safetensors",
            "50s Noir Movie Still",
            1.0,
            "옛날 느와르", "고전 느와르", "50년대 느와르"
    ),

    Baroque_Apocalypse(
            "BaroqueApocalypseKlein9b.safetensors",
            "Baroque Apocalypse",
            1.0,
            "바로크 아포칼립스", "고딕 아포칼립스", "다크판타지"
    ),

    Sci_Fi_Movie(
            "2020sSciFiKlein9b.safetensors",
            "ArsMovieStill, Movie still from a 2020s Sci-Fi movie",
            1.0,
            "2020년 SF영화", "현대 SF영화", "요즘 SF영화"
    ),

    PSYCHEDELIC_60S(
            "60sPsyKlein9b.safetensors",
            "Colored pencil hyperdetailed realism",
            0.9,
            "60년대 사이케델릭", "히피풍", "옛날 컬러영화", "60년대 스타일", "60년대풍"
    ),

    Hyperdetailed_Colored_Pencil(
            "HyperDetailedPencilperKlein9b_000002000H1000.safetensors",
            "Colored pencil hyperdetailed realism",
            1.0,
            "디테일 색연필", "사실주의 색연필", "고퀄리티 색연필", "고퀄 색연필"
    ),

    FANTASY_80S(
            "80sFantasyKlein9b_000002000H1000.safetensors",
            "ArsMovieStill, 80s Fantasy Movie Still",
            1.0,
            "80년대 판타지", "옛날 판타지 영화"
    );

    // ──────────────────────────────────────────────────────────────────────────

    public final String   filename;
    public final String   triggerWords;
    public final double   strength;
    public final String[] keywords;

    LoraConfig(String filename, String triggerWords, double strength, String... keywords) {
        this.filename     = filename;
        this.triggerWords = triggerWords;
        this.strength     = strength;
        this.keywords     = keywords;
    }

    /**
     * 사용자 메시지에서 매칭되는 LoRA를 찾아 반환합니다.
     * 대소문자·공백 무관, 첫 번째 매칭 LoRA를 반환합니다.
     *
     * @param userMessage 사용자의 원문 메시지
     * @return 매칭된 LoRA, 없으면 Optional.empty()
     */
    public static Optional<LoraConfig> detect(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return Optional.empty();
        String lower = userMessage.toLowerCase().replaceAll("\\s+", " ").trim();
        return Arrays.stream(values())
                .filter(lora -> Arrays.stream(lora.keywords)
                        .anyMatch(kw -> lower.contains(kw.toLowerCase())))
                .findFirst();
    }

    /**
     * 선택된 LoRA에 트리거 단어를 붙인 최종 프롬프트를 생성합니다.
     *
     * @param basePrompt analyzeRequestWithAi가 번역한 영어 프롬프트
     * @return triggerWords + ", " + basePrompt
     */
    public String buildPrompt(String basePrompt) {
        if (basePrompt == null || basePrompt.isBlank()) return triggerWords;
        return triggerWords + ", " + basePrompt;
    }
}