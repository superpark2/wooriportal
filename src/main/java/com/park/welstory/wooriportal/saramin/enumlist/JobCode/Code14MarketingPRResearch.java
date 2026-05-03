package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code14MarketingPRResearch {

    LICENSING(2244, "라이센싱"),
    PPL(2243, "PPL"),
    CBO(2242, "CBO"),
    PR(2210, "홍보"),
    MARKETING(2201, "마케팅"),
    HOSPITAL_MARKETING(1411, "병원마케팅"),
    SEARCH_AD(1449, "검색광고"),
    MARKETING_PLANNING(1412, "마케팅기획"),
    ADVERTISER_MANAGEMENT(1450, "광고주관리"),
    MARKETING_STRATEGY(1413, "마케팅전략"),
    AD_CAMPAIGN(1451, "광고캠페인"),
    BLOG_MARKETING(1414, "블로그마케팅"),
    GROWTH_HACKING(1452, "그로스해킹"),
    MEDIA_MANAGEMENT(1453, "매체관리"),
    SPORTS_MARKETING(1415, "스포츠마케팅"),
    BANNER_AD(1454, "배너광고"),
    INFLUENCER_MARKETING(1416, "인플루언서마케팅"),
    BIDDING(1455, "비딩"),
    EXPERIENTIAL_MARKETING(1417, "체험마케팅"),
    EVENT_PLANNING(1418, "행사기획"),
    COMPANY_NEWSLETTER(1456, "사보뉴스레터"),
    SOCIAL_SURVEY(1457, "사회조사"),
    SNS_MARKETING(1419, "SNS마케팅"),
    SURVEY(1458, "설문조사"),
    CONTENT_PLANNING(1420, "콘텐츠기획"),
    AD_MARKETING(1421, "광고마케팅"),
    SALES_PROMOTION(1459, "세일즈프로모션"),
    AD_PD(1422, "광고PD"),
    MARKET_RESEARCH(1460, "시장조사"),
    MEDIA_PR(1461, "언론홍보"),
    GLOBAL_MARKETING(1423, "글로벌마케팅"),
    OUTDOOR_AD(1462, "옥외광고"),
    CORPORATE_PR(1424, "기업홍보"),
    EVENT_PROMOTION(1463, "이벤트프로모션"),
    DIGITAL_MARKETING(1425, "디지털마케팅"),
    MOBILE_MARKETING(1426, "모바일마케팅"),
    CHANNEL_MANAGEMENT(1464, "채널관리"),
    KEYWORD_AD(1465, "키워드광고"),
    MEDIA_PLANNER(1427, "미디어플래너"),
    STATISTICAL_ANALYSIS(1466, "통계분석"),
    VIRAL_MARKETING(1428, "바이럴마케팅"),
    BRAND_MARKETING(1429, "브랜드마케팅"),
    PUBLICITY(1467, "퍼블리시티"),
    BUSINESS_MARKETING(1430, "비즈니스마케팅"),
    ATL(1468, "ATL"),
    OFFLINE_MARKETING(1431, "오프라인마케팅"),
    BTL(1469, "BTL"),
    ONLINE_MARKETING(1432, "온라인마케팅"),
    IMC(1470, "IMC"),
    AFFILIATE_MARKETING(1433, "제휴마케팅"),
    MCN(1471, "MCN"),
    MICE(1472, "MICE"),
    SURVEYOR(1434, "조사원"),
    RFP(1473, "RFP(제안요청서)"),
    CONTENT_MARKETING(1435, "콘텐츠마케팅"),
    SEO(1474, "SEO"),
    CONTENT_EDITOR(1436, "콘텐츠에디터"),
    PERFORMANCE_MARKETING(1437, "퍼포먼스마케팅"),
    PRODUCT_MARKETING(1438, "프로덕트마케팅"),
    ART_DIRECTOR(1439, "AD(아트디렉터)"),
    AD_PLANNER(1440, "AE(광고기획자)"),
    ACCOUNT_MANAGER(1441, "AM(어카운트매니저)"),
    B2B_MARKETING(1442, "B2B마케팅"),
    BRAND_MANAGER(1443, "BM(브랜드매니저)"),
    CREATIVE_DIRECTOR(1444, "CD(크리에이티브디렉터)"),
    CMO(1445, "CMO"),
    CRM_MARKETING(1446, "CRM마케팅"),
    COPYWRITER(1447, "CW(카피라이터)"),
    MEDICAL_WRITER(1448, "MW(메디컬라이터)");

    private final int code;
    private final String name;

    Code14MarketingPRResearch(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code14MarketingPRResearch fromCode(int code) {
        for (Code14MarketingPRResearch c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}