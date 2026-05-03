package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code16TradeLogistics {

    RISK_MANAGEMENT(2259, "리스크 관리"),
    BUSINESS_MANAGEMENT(2225, "경영관리"),
    CSO(2258, "CSO"),
    CIO(2257, "CIO"),
    PLANNING(2205, "기획"),
    GAME_PLANNING(1624, "게임기획"),
    BUSINESS_ANALYSIS(1652, "경영분석"),
    MANAGEMENT_PLANNING(1625, "경영기획"),
    MANAGEMENT_CONSULTING(1653, "경영컨설팅"),
    AD_PLANNING(1626, "광고기획"),
    MANAGEMENT_INNOVATION(1654, "경영혁신(PI)"),
    EDUCATION_PLANNING(1627, "교육기획"),
    FINANCIAL_CONSULTING(1655, "금융컨설팅"),
    LEVEL_DESIGN(1656, "레벨디자인"),
    TECH_PLANNING(1628, "기술기획"),
    RESEARCH(1657, "리서치"),
    MARKETING_PLANNING(1629, "마케팅기획"),
    DATA_ANALYSIS(1658, "데이터분석"),
    CULTURE_PLANNING(1630, "문화기획"),
    BUSINESS_DEVELOPMENT(1659, "사업개발"),
    BRANCH_MANAGER(1631, "법인장"),
    BUSINESS_OPERATION(1660, "사업관리"),
    BRAND_PLANNING(1632, "브랜드기획"),
    BUSINESS_ALLIANCE(1661, "사업제휴"),
    BUSINESS_PLANNING(1633, "사업기획"),
    PRODUCT_PLANNING(1634, "상품기획"),
    STORYBOARD(1662, "스토리보드"),
    SERVICE_PLANNING(1635, "서비스기획"),
    MARKET_RESEARCH(1663, "시장조사"),
    NEW_BUSINESS_PLANNING(1664, "신사업기획"),
    APP_PLANNING(1636, "앱기획"),
    NEW_BUSINESS_DISCOVERY(1665, "신사업발굴"),
    WEB_PLANNING(1637, "웹기획"),
    PERFORMANCE_MANAGEMENT(1666, "실적관리"),
    HR_PLANNING(1638, "인사기획"),
    ACCELERATING(1667, "엑셀러레이팅"),
    STRATEGY_PLANNING(1639, "전략기획"),
    BUDGET_MANAGEMENT(1668, "예산관리"),
    BRANCH_MANAGEMENT(1640, "지점관리자"),
    INCUBATING(1669, "인큐베이팅"),
    PUBLISHING_PLANNING(1641, "출판기획"),
    CONSULTANT(1642, "컨설턴트"),
    DATA_RESEARCH(1670, "자료조사"),
    EVENT_PLANNING(1643, "행사기획"),
    ORGANIZATION_MANAGEMENT(1671, "조직관리"),
    CEO(1644, "CEO"),
    SUSTAINABILITY_MANAGEMENT(1672, "지속가능경영"),
    COO(1645, "COO"),
    STARTUP_CONSULTING(1673, "창업컨설팅"),
    CTO(1646, "CTO"),
    FEASIBILITY_REVIEW(1674, "타당성검토"),
    IT_CONSULTING(1647, "IT컨설팅"),
    INVESTMENT_STRATEGY(1675, "투자전략"),
    PL(1648, "PL(프로젝트리더)"),
    TREND_ANALYSIS(1676, "트렌드분석"),
    PROTOTYPING(1677, "프로토타이핑"),
    PM(1649, "PM(프로젝트매니저)"),
    OVERSEAS_CORPORATION_MANAGEMENT(1678, "해외법인관리"),
    PMO(1650, "PMO"),
    BPR(1679, "BPR"),
    PO(1651, "PO(프로덕트오너)"),
    BSC(1680, "BSC"),
    CSR(1681, "CSR"),
    ESG(1682, "ESG"),
    ISMP(1683, "ISMP"),
    ISP(1684, "ISP"),
    KPI_MANAGEMENT(1685, "KPI관리"),
    MA(1686, "M&A"),
    MBO(1687, "MBO"),
    OKR(1688, "OKR"),
    RFP(1689, "RFP(제안요청서)"),
    UIUX(1690, "UIUX");

    private final int code;
    private final String name;

    Code16TradeLogistics(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code16TradeLogistics fromCode(int code) {
        for (Code16TradeLogistics c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}