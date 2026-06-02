package com.mrpark.dev.wooriportal.hrd;

/** HRD 응답이 SSV 가 아님(로그인 페이지/리다이렉트) → 세션 만료/무효 추정. */
public class HrdSessionExpiredException extends RuntimeException {
    public HrdSessionExpiredException(String message) {
        super(message);
    }
}
