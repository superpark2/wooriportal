package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code17BusinessPlanningManagement {

    MUTUAL_AID_INSTITUTION(1738, "공제기관"),
    CORPORATE_FINANCE(1703, "기업금융"),
    LOAN_CONSULTANT(1691, "대출상담사"),
    INSURANCE_PLANNER(1692, "보험설계사"),
    PRIVATE_FINANCE(1739, "사금융권"),
    CORPORATE_ANALYSIS(1704, "기업분석"),
    LOSS_ADJUSTER(1693, "손해사정사"),
    LIFE_INSURANCE(1740, "생명보험사"),
    CORPORATE_REVIEW(1705, "기업심사"),
    FUTURES_BROKERAGE(1741, "선물중개회사"),
    CREDIT_ANALYST(1694, "심사역"),
    COLLATERAL_LOAN(1706, "담보대출"),
    ANALYST(1695, "애널리스트"),
    LOAN_REVIEW(1707, "대출심사"),
    NON_LIFE_INSURANCE(1742, "손해보험사"),
    TELLER(1696, "텔러"),
    COMPENSATION(1708, "배상"),
    COMMERCIAL_BANK(1743, "일반은행"),
    ASSET_MANAGEMENT_COMPANY(1744, "자산운용사"),
    LIABILITY_COMPENSATION(1709, "배상책임"),
    FUND_MANAGER(1697, "펀드매니저"),
    SECOND_FINANCIAL_SECTOR(1745, "제2금융권"),
    INSURANCE_ACCIDENT(1710, "보험사고"),
    FINANCIAL_AFFAIRS(1698, "금융사무"),
    INSURANCE_CLAIM(1711, "보험청구"),
    SECURITIES_COMPANY(1746, "증권사"),
    FINANCIAL_PRODUCT_SALES(1699, "금융상품영업"),
    INVESTMENT_ADVISORY(1747, "투자자문사"),
    REAL_ESTATE_INVESTMENT(1712, "부동산투자"),
    INSURANCE_CONSULTING(1700, "보험상담"),
    SPECIAL_BANK(1748, "특수은행"),
    NON_LIFE_INSURANCE_PRODUCT(1713, "손해보험"),
    INSURANCE_PRODUCT_DEVELOPMENT(1701, "보험상품개발"),
    SAVINGS_BANK(1749, "저축은행"),
    INSURANCE_REVIEW(1702, "보험심사"),
    LOSS_ASSESSMENT(1714, "손해평가"),
    TRUST(1715, "신탁"),
    CREDIT_REVIEW(1716, "여신심사"),
    FOREIGN_EXCHANGE_MANAGEMENT(1717, "외환관리"),
    RISK_MANAGEMENT(1718, "위험관리"),
    RISK_ANALYSIS(1719, "위험분석"),
    ASSET_MANAGEMENT(1720, "자산운용"),
    FINANCIAL_ANALYSIS(1721, "재무분석"),
    PROPERTY_LOSS_ADJUSTMENT(1722, "재물손해사정"),
    STOCK_SALES(1723, "주식영업"),
    STOCK_INVESTMENT(1724, "주식투자"),
    BOND_MANAGEMENT(1725, "채권관리"),
    DEBT_COLLECTION(1726, "채권추심"),
    INVESTMENT_REVIEW(1727, "투자검토"),
    INVESTMENT_ANALYSIS(1728, "투자분석"),
    INVESTMENT_SCREENING(1729, "투자심사"),
    INVESTMENT_ADVISORY2(1730, "투자자문"),
    INVESTMENT_ASSETS(1731, "투자자산"),
    FUND(1732, "펀드"),
    CURRENCY_EXCHANGE(1733, "환전"),
    DCM(1734, "DCM"),
    ECM(1735, "ECM"),
    NPL(1736, "NPL"),
    PF_SALES(1737, "PF영업");

    private final int code;
    private final String name;

    Code17BusinessPlanningManagement(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code17BusinessPlanningManagement fromCode(int code) {
        for (Code17BusinessPlanningManagement c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}