package com.mrpark.dev.wooriportal.hrd.session;

import java.time.Instant;
import lombok.Getter;

/**
 * 프록시 하베스트로 수확한, 살아있는 HRD-Net 세션 자격증명.
 *
 * <p>HRD 데스크탑 행정프로그램이 보유한 쿠키를 미러링한 값.
 * 실제 서버 호출 시 {@code Cookie} 헤더로 재생한다.</p>
 */
@Getter
public class HrdSession {

    private final String jsessionId;
    private final String wmonid;
    private final Instant harvestedAt;

    public HrdSession(String jsessionId, String wmonid, Instant harvestedAt) {
        this.jsessionId = jsessionId;
        this.wmonid = wmonid;
        this.harvestedAt = harvestedAt;
    }

    /** HRD 요청에 실을 Cookie 헤더 값. */
    public String toCookieHeader() {
        StringBuilder sb = new StringBuilder("gv_ssoFlag=; ");
        if (wmonid != null && !wmonid.isBlank()) {
            sb.append("WMONID=").append(wmonid).append("; ");
        }
        sb.append("JSESSIONID=").append(jsessionId).append(";");
        return sb.toString();
    }

    /** 같은 자격증명인지(변경 감지용). */
    public boolean sameCredentials(String otherJsession, String otherWmonid) {
        return java.util.Objects.equals(jsessionId, otherJsession)
                && java.util.Objects.equals(wmonid, otherWmonid);
    }
}
