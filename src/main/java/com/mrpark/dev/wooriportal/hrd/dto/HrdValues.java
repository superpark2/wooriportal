package com.mrpark.dev.wooriportal.hrd.dto;

/** HRD-Net 원시 문자열 값(yyyyMMdd, HHmm, 정수문자열 등) 표시/변환 헬퍼. */
final class HrdValues {

    private HrdValues() {
    }

    /** "20260602" → "2026-06-02". 형식이 다르면 원문 그대로 반환. */
    static String formatDate(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) {
            return yyyymmdd;
        }
        return yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8);
    }

    /** "1900" → "19:00". 형식이 다르면 원문 그대로 반환. */
    static String formatTime(String hhmm) {
        if (hhmm == null || hhmm.length() != 4) {
            return hhmm;
        }
        return hhmm.substring(0, 2) + ":" + hhmm.substring(2, 4);
    }

    /** 정수 문자열 → Integer. 비거나 숫자가 아니면 null. */
    static Integer toInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
