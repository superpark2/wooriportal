package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code3FinanceAccounting {

    ADMINISTRATIVE_AGENT(2238, "행정사"),
    FINANCE(2220, "재무"),
    ACCOUNTING(2197, "회계"),
    DUZON(335, "더존"),
    AUDIT(321, "감사"),
    CUSTOMS_CORPORATION(376, "관세법인"),
    ACCOUNTING_CLERK(322, "경리"),
    FOUR_MAJOR_INSURANCE(336, "4대보험"),
    CUSTOMS_OFFICE(377, "세관"),
    TAX_CORPORATION(378, "세무법인"),
    ACCOUNTING_STAFF(323, "경리사무원"),
    INVOICE_ISSUANCE(337, "계산서발행"),
    TAX_OFFICE(379, "세무사사무실"),
    CPA(324, "공인회계사"),
    MANAGEMENT_ACCOUNTING(338, "관리회계"),
    OVERSEAS_CORPORATION(380, "해외법인"),
    CUSTOMS_BROKER(325, "관세사"),
    PAYROLL(339, "급여(Payroll)"),
    ACCOUNTING_CORPORATION(381, "회계법인"),
    CUSTOMS_CLERK(326, "관세사무원"),
    CORPORATE_ACCOUNTING(340, "기업회계"),
    INTERNAL_AUDIT(341, "내부감사"),
    ACCOUNTING_OFFICE(382, "회계사무실"),
    TAX_ACCOUNTANT(327, "세무사"),
    CORPORATE_SETTLEMENT(342, "법인결산"),
    COMPUTERIZED_ACCOUNTING(328, "전산회계"),
    CORPORATE_TAX_FILING(343, "법인세신고"),
    ACCOUNTANT(329, "회계사"),
    VAT_FILING(344, "부가세신고"),
    AICPA(330, "AICPA"),
    TAX_BOOKKEEPING(345, "세무기장"),
    CFA(331, "CFA"),
    TAX_FILING(346, "세무신고"),
    CFO(332, "CFO"),
    TAX_ADJUSTMENT(347, "세무조정"),
    IR_DISCLOSURE(333, "IR공시"),
    KICPA(334, "KICPA"),
    TAX_CONSULTING(348, "세무컨설팅"),
    TAX_ACCOUNTING(349, "세무회계"),
    PROFIT_LOSS_MANAGEMENT(350, "손익관리"),
    FILING_AGENT(351, "신고대리"),
    CONSOLIDATED_ACCOUNTING(352, "연결회계"),
    YEAR_END_TAX_SETTLEMENT(353, "연말정산"),
    BUDGET_MANAGEMENT(354, "예산관리"),
    EXTERNAL_AUDIT(355, "외부감사"),
    FOREIGN_EXCHANGE_MANAGEMENT(356, "외환관리"),
    COST_MANAGEMENT(357, "원가관리"),
    COST_ACCOUNTING(358, "원가회계"),
    WITHHOLDING_TAX_FILING(359, "원천세신고"),
    FUND_MANAGEMENT(360, "자금관리"),
    ASSET_MANAGEMENT(361, "자산관리"),
    ASSET_OPERATION(362, "자산운용"),
    SELF_BOOKKEEPING(363, "자체기장"),
    FINANCIAL_PLANNING(364, "재무기획"),
    FINANCIAL_STATEMENTS(365, "재무제표"),
    FINANCIAL_ACCOUNTING(366, "재무회계"),
    SLIP_ENTRY(367, "전표입력"),
    GLOBAL_INCOME_TAX(368, "종합소득세"),
    RECEIVABLES_MANAGEMENT(369, "채권관리"),
    CASHIERING(370, "출납"),
    ACCOUNTING_SETTLEMENT(371, "회계결산"),
    ERP(372, "ERP"),
    IFRS(373, "IFRS"),
    IPO(374, "IPO"),
    NDR(375, "NDR");

    private final int code;
    private final String name;

    Code3FinanceAccounting(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code3FinanceAccounting fromCode(int code) {
        for (Code3FinanceAccounting c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}