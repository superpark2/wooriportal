package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code4AdministrationLegal {

    LEGAL(2215, "법무"),
    JUDICIAL_SCRIVENER(383, "법무사"),
    EDUCATION_ADMINISTRATION(399, "교육행정"),
    PATENT_ATTORNEY(384, "변리사"),
    TECH_COMMERCIALIZATION(400, "기술사업화"),
    LAWYER(385, "변호사"),
    VISITOR_RECEPTION(401, "내방객응대"),
    SECRETARY(386, "비서"),
    DOCUMENT_WRITING(402, "문서작성"),
    IN_HOUSE_LAWYER(387, "사내변호사"),
    SUPPLIES_MANAGEMENT(403, "비품관리"),
    OFFICE_WORKER(388, "사무직"),
    COMPANY_EVENT(404, "사내행사"),
    CLERICAL_STAFF(389, "서무"),
    OFFICE_ASSISTANT(405, "사무보조"),
    OFFICE_ADMINISTRATION(406, "사무행정"),
    LITIGATION_SECRETARY(390, "송무비서"),
    LEGAL_CLERK(391, "법률사무원"),
    SITE_MANAGEMENT(407, "사이트관리"),
    CHAUFFEUR(392, "수행기사"),
    TRADEMARK_MANAGEMENT(408, "상표관리"),
    EXECUTIVE_DRIVER_SECRETARY(393, "수행비서"),
    DOCUMENT_MANAGEMENT(409, "서류관리"),
    INFORMATION_DESK(394, "안내데스크"),
    FACILITY_MANAGEMENT(410, "시설관리"),
    EXECUTIVE_SECRETARY(395, "임원비서"),
    LICENSE_PERMIT(411, "인허가"),
    GENERAL_AFFAIRS(396, "총무"),
    DATA_ENTRY(412, "자료입력"),
    COMPLIANCE(397, "컴플라이언스"),
    DATA_RESEARCH(413, "자료조사"),
    ASSET_MANAGEMENT(414, "자산관리"),
    PATENT_SPECIFICATION_AGENT(398, "특허명세사"),
    COMPUTERIZED_GENERAL_AFFAIRS(415, "전산총무"),
    PHONE_RECEPTION(416, "전화응대"),
    CERTIFICATE_ISSUANCE(417, "제증명발급"),
    CONTENT_MANAGEMENT(418, "콘텐츠관리"),
    TYPING(419, "타이핑"),
    PATENT_MANAGEMENT(420, "특허관리"),
    PATENT_ANALYSIS(421, "특허분석"),
    PATENT_CONSULTING(422, "특허컨설팅"),
    EXCEL(423, "Excel"),
    IP(424, "IP(지식재산권)"),
    OA(425, "OA"),
    PHOTOSHOP(426, "PhotoShop"),
    POWERPOINT(427, "PowerPoint"),
    INFORMATION_PROCESSING(428, "정보처리"),
    MANAGEMENT_SUPPORT(429, "경영지원");

    private final int code;
    private final String name;

    Code4AdministrationLegal(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code4AdministrationLegal fromCode(int code) {
        for (Code4AdministrationLegal c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}