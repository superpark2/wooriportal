package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code21CustomerConsultingTM {

    BULLETIN_BOARD_MANAGEMENT(1980, "게시판관리"),
    COUNSELOR(1972, "상담원"),
    CUSTOMER_MANAGEMENT(1981, "고객관리"),
    OUTBOUND(1973, "아웃바운드"),
    IMAGE_CONSULTANT(1974, "이미지컨설턴트"),
    EDUCATION_CONSULTING(1982, "교육상담"),
    INBOUND(1975, "인바운드"),
    EXCHANGE_RETURN(1983, "교환반품"),
    TELEMARKETER(1976, "텔레마케터"),
    TECHNICAL_CONSULTING(1984, "기술상담"),
    CS(1977, "CS"),
    SIMPLE_GUIDANCE(1985, "단순안내"),
    CX_MANAGER(1978, "CX매니저"),
    LOAN_CONSULTING(1986, "대출상담"),
    SOLICITATION_TM(1979, "섭외TM"),
    MAIL_CONSULTING(1987, "메일상담"),
    CIVIL_COMPLAINT_CONSULTING(1988, "민원상담"),
    VISIT_CONSULTING(1989, "방문상담"),
    DELIVERY_CONSULTING(1990, "배송상담"),
    CONSULTING_QUALITY_MANAGEMENT(1991, "상담품질관리"),
    REMOTE_CONSULTING(1992, "원격상담"),
    PHONE_CONSULTING(1993, "전화상담"),
    RECEPTION_RESERVATION(1994, "접수예약"),
    ORDER_CONSULTING(1995, "주문상담"),
    CHAT_CONSULTING(1996, "채팅상담"),
    CALL_CENTER_CUSTOMER_CENTER(1997, "콜센터고객센터"),
    CALL_STATISTICS_ANALYSIS(1998, "콜통계분석"),
    CALL_QUALITY_ANALYSIS(1999, "통화품질분석"),
    CHURN_DEFENSE(2000, "해지방어"),
    HAPPY_CALL(2001, "해피콜"),
    AS_CONSULTING(2002, "AS상담"),
    VOC_ANALYSIS(2003, "VOC분석");

    private final int code;
    private final String name;

    Code21CustomerConsultingTM(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code21CustomerConsultingTM fromCode(int code) {
        for (Code21CustomerConsultingTM c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}