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

    NIJI(
            "Niji2Klein4B.safetensors",
            "ArsNijiStyle,",
            1.0,
            "니지스타일", "니지풍"
    ),

    CRIME_THRILLER(
            "2000sCrimeThrillerKlein4B.safetensors",
            "ArsMovieStill, movie still from a gritty, high-contrast 2000s crime thriller movie",
            0.9,
            "2000년대 범죄영화", "옛날 범죄영화", "고전 범죄영화"
    ),

    FOLK_HORROR(
            "FolkHorrorKlein4B.safetensors",
            "ArsMovieStill, Movie still from a 2010s folklore horror film",
            1.0,
            "포크호러"
    ),

    NOIR(
            "50sNoirKlein4B.safetensors",
            "50s Noir Movie Still",
            1.0,
            "느와르"
    ),

    DISNEY(
            "DisneyMidCenturyKlein4B_000005000.safetensors",
            "Disney Mid-Century Animation",
            0.9,
            "2d디즈니", "디즈니2d", "옛날 디즈니", "디즈니 만화", "디즈니 카툰"
    ),

    INDIE_90S(
            "90sIndieiKlein4B_000003500.safetensors",
            "ArsMovieStill, movie still from a 1990s indie movie",
            1.0,
            "90년대 영화", "인디영화", "90s 영화"
    ),

    Pixel_art(
            "Pixel_art_people_S.Klein4B_epoch_10.safetensors",
            "pixel art",
            1.0,
            "픽셀화", "픽셀아트", "도트화"
    ),

    Aesthetic(
            "klein4b_masterpieces_v3.1.safetensors",
            "masterpiece, very aesthetic",
            1.0,
            "루미나베일"
    ),

    Midjourney(
            "RebelMidjourney (KLEIN 4B).safetensors",
            "",
            1.0,
            "미드저니"
    ),

    ComicStyle(
            "klein_slider_comic.safetensors",
            "NewMecha style illustration. Highly detailed digital illustration in a semi-realistic anime style.",
            1.0,
            "코믹", "코믹북", "미국만화", "미국 만화"
    ),

    FANTASY_80S(
            "80sFantasyKlein4b2000H1000.safetensors",
            "ArsMovieStill, 80s Fantasy Movie Still",
            1.0,
            "80년대 판타지", "옛날 판타지 영화"
    ),

    ETHEREAL_GOTHIC(
            "EtherialGothicKlein4BF.safetensors",
            "ArsMJStyle, Etherial Gothic",
            1.0,
            "에테르 고딕", "몽환적인 고딕"
    ),

    DARK_GHIBLI(
            "DarkGhibliKlein4BF_000005000.safetensors",
            "Studio Ghibli Dark Fairytale",
            1.0,
            "다크 지브리", "잔혹동화", "다크 페어리"
    ),

    SCIFI_70S(
            "70sSciFiKlein4B.safetensors",
            "ArsMovieStill, movie still from a 1970s technicolor Sci-Fi movie",
            1.0,
            "70년대 공상과학", "70년대 SF", "테크니컬러"
    ),

    TECHNICOLOR_30S(
            "30sTechnocoloriKlein4B.safetensors",
            "ArsMovieStill, movie still from a 1930s technicolor movie",
            0.9,
            "30년대 영화", "옛날 컬러영화"
    ),

    PANAVISION_50S(
            "50sPanavisionKlein4B.safetensors",
            "ArsMovieStill, Movie Still From Colored 1950s Super Panavision 70 Movie",
            0.9,
            "50년대 영화", "파나비전"
    ),

    PSYCHEDELIC_60S(
            "60sPsyKlein4B.safetensors",
            "ArsMovieStill, movie still from a 60s psychedelic movie",
            0.9,
            "60년대 사이케델릭", "히피풍"
    ),

    DND_DARKEST(
            "DarkestDnD2Klein4B.safetensors",
            "dnddarkestfantasy",
            1.0,
            "던전앤드래곤", "다크 판타지", "DND"
    ),

    SCIFI_2020S(
            "2020sSciFiKlein4B.safetensors",
            "ArsMovieStill, Movie still from a 2020s Sci-Fi movie",
            1.0,
            "최신 SF영화", "현대적인 SF", "현대 SF영화"
    ),

    GOTHIC_ELEGANCE(
            "EtherialGothicEleganceKlein4B.safetensors",
            "Ethereal Gothic Elegance",
            1.0,
            "고딕 엘레강스", "우아한 고딕"
    ),

    HYPER_ILLUST(
            "HyperdetailedIllustrationKlein4B.safetensors",
            "ArsMJStyle, HyperDetailed Illustration,",
            1.0,
            "극사실주의 일러스트", "디테일 일러스트", "하이퍼 일러스트"
    ),

    ASIAN_MIX(
            "hina_flux2Klein4b_asianMix_v2.7-lora.safetensors",
            "asian woman",
            1.0,
            "아시아 믹스", "동양인", "동양 여자", "아시아 여자"
    ),

    GAMEBOOK(
            "Gamebook-illustration-v4-(Flux2).safetensors",
            "CYOA, a black and white, monochrome illustration in the style of old fantasy gamebooks and vintage fantasy role-playing games.",
            1.0,
            "게임북", "빈티지 RPG", "삽화", "흑백 삽화"
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