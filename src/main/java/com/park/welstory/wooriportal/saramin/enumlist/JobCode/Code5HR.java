package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code5HR {

    HR(2198, "인사"),
    LABOR_ATTORNEY(430, "노무사"),
    PAYROLL(439, "급여(Payroll)"),
    RECRUITER(431, "채용담당자"),
    INTERVIEW(440, "면접인터뷰"),
    JOB_MANAGER(432, "잡매니저"),
    MANDATORY_EDUCATION(441, "법정의무교육"),
    VOCATIONAL_COUNSELOR(433, "직업상담사"),
    WELFARE_BENEFITS(442, "복리후생"),
    HEADHUNTER(434, "헤드헌터"),
    PERFORMANCE_MANAGEMENT(443, "실적관리"),
    OUTSOURCING(444, "아웃소싱"),
    ER(435, "ER(노무관리)"),
    HR_CONSULTING(436, "HR컨설팅"),
    ONBOARDING(445, "온보딩"),
    HRD(437, "HRD"),
    WORKFORCE_MANAGEMENT(446, "인력관리"),
    HR_TRAINING(447, "인사교육"),
    HRM(438, "HRM"),
    HR_PLANNING(448, "인사기획"),
    HR_ADMINISTRATION(449, "인사행정"),
    TALENT_SCOUTING(450, "인재발굴"),
    WAGE_NEGOTIATION(451, "임금협상"),
    CERTIFICATE_ISSUANCE(452, "제증명발급"),
    ORGANIZATIONAL_CULTURE(453, "조직문화"),
    VOCATIONAL_TRAINING(454, "직업훈련"),
    RECRUITMENT_POSTING(455, "채용공고관리"),
    RECRUITMENT_AGENCY(456, "채용대행"),
    RECRUITMENT_FAIR(457, "채용설명회"),
    DISPATCH_MANAGEMENT(458, "파견관리"),
    EVALUATION_COMPENSATION(459, "평가보상");

    private final int code;
    private final String name;

    Code5HR(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code5HR fromCode(int code) {
        for (Code5HR c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}