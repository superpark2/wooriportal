package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code9ResearchRND {

    SEMICONDUCTOR(2217, "반도체"),
    AIR_MEASUREMENT_ANALYST(799, "대기측정분석사"),
    POLYMER(812, "고분자"),
    ROBOT_ENGINEER(800, "로봇엔지니어"),
    OPTICAL_DESIGN(813, "광학설계"),
    RESEARCHER(801, "연구원"),
    TECHNICAL_RESEARCH(814, "기술연구"),
    CLIMATE_CHANGE(815, "기후변화"),
    CERTIFICATION_AUDITOR(802, "인증심사원"),
    AGRICULTURE(816, "농업"),
    CLINICAL_DM(803, "임상DM"),
    CLINICAL_PM(804, "임상PM"),
    PAINT_COATING(817, "도료페인트"),
    CLINICAL_STAT(805, "임상STAT"),
    ANIMAL_TESTING(818, "동물실험"),
    ENVIRONMENTAL_ANALYST(806, "환경측정분석사"),
    ROBOT_DESIGN(819, "로봇설계"),
    CRA(807, "CRA(임상연구원)"),
    MENU_DEVELOPMENT(820, "메뉴개발"),
    CRC(808, "CRC(임상시험코디네이터)"),
    DRONE(821, "무인항공기드론"),
    CRM(809, "CRM(임상연구전문가)"),
    MICROBIOLOGY(822, "미생물"),
    RND(810, "R&D"),
    VIRUS(823, "바이러스"),
    MOLECULAR_DIAGNOSIS(824, "분자진단"),
    RND_PLANNING(811, "R&D기획"),
    LIFE_SCIENCE(825, "생명과학"),
    CELL_CULTURE(826, "세포배양"),
    CELL_EXPERIMENT(827, "세포실험"),
    WATER_QUALITY_ANALYSIS(828, "수질분석"),
    SAMPLE_ANALYSIS(829, "시료분석"),
    SAMPLE_COLLECTION(830, "시료채취"),
    FOOD_RESEARCH(831, "식품연구"),
    NEW_MATERIAL(832, "신소재"),
    RENEWABLE_ENERGY(833, "신재생에너지"),
    LAB_ASSISTANT(834, "실험보조"),
    ALGORITHM_DEVELOPMENT(835, "알고리즘개발"),
    NUCLEAR_ENERGY(836, "원자력"),
    ORGANIC_SYNTHESIS(837, "유기합성"),
    GENE(838, "유전자"),
    HAZARDOUS_CHEMICALS(839, "유해화학물질"),
    MEDICAL_DEVICE_RESEARCH(840, "의료기기연구"),
    QUASI_DRUG_RESEARCH(841, "의약외품연구"),
    IMAGE_PROCESSING(842, "이미지프로세싱"),
    PHYSICOCHEMICAL_TEST(843, "이화학시험"),
    CLINICAL_DEVELOPMENT(844, "임상개발"),
    CLINICAL_TRIAL(845, "임상시험"),
    AUTONOMOUS_DRIVING(846, "자율주행"),
    ELECTROMAGNETIC_WAVE(847, "전자파"),
    POLICY_RESEARCH(848, "정책연구"),
    PHARMA_BIO(849, "제약바이오"),
    FORMULATION_RESEARCH(850, "제제연구"),
    DOSAGE_FORM_RESEARCH(851, "제형연구"),
    STEM_CELL(852, "줄기세포"),
    SOIL_ENVIRONMENT(853, "토양환경"),
    ACADEMIC_RESEARCH(854, "학술연구"),
    ENVIRONMENTAL_POLLUTION(855, "환경오염"),
    AI(856, "AI(인공지능)"),
    FT_IR_ANALYSIS(857, "FT-IR분석");

    private final int code;
    private final String name;

    Code9ResearchRND(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code9ResearchRND fromCode(int code) {
        for (Code9ResearchRND c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}