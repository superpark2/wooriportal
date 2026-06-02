package com.mrpark.dev.wooriportal.hrd;

import static org.assertj.core.api.Assertions.assertThat;

import com.mrpark.dev.wooriportal.hrd.session.HrdSession;
import com.mrpark.dev.wooriportal.hrd.session.HrdSessionStore;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvData;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvDecoder;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 요청 빌드(순수) 검증 — 네트워크 없이 템플릿 치환이 올바른지. */
class HrdNetClientTest {

    @Test
    void buildsDetailRequestWithSwappedSessionAndKeys() {
        byte[] template = HrdCaptures.load("/hrd/selectDailAtndceDetail.req.hex");
        HrdNetClient client = new HrdNetClient(new HrdSessionStore());
        HrdSession session = new HrdSession("LIVESESSION!-9!-9", "WMONLIVE", Instant.now());

        byte[] body = client.buildDetailRequestBody(template, session,
                "AIG20250000541286", "1", "20260603");

        // FF AD 프레이밍 + 디코딩
        assertThat(body[0] & 0xFF).isEqualTo(0xFF);
        assertThat(body[1] & 0xFF).isEqualTo(0xAD);
        SsvData decoded = SsvDecoder.decode(body);

        // 세션/타임스탬프 주입
        assertThat(decoded.getVariable("JSESSIONID")).isEqualTo("LIVESESSION!-9!-9");
        assertThat(decoded.getVariable("WMONID")).isEqualTo("WMONLIVE");
        assertThat(decoded.getVariable("request-timestamp")).hasSize(14);

        // 조회키 교체
        assertThat(decoded.getDataset("ds_cond").getString(0, "tracseId")).isEqualTo("AIG20250000541286");
        assertThat(decoded.getDataset("ds_cond").getString(0, "tracseTme")).isEqualTo("1");
        assertThat(decoded.getDataset("ds_cond").getString(0, "traingDe")).isEqualTo("20260603");

        // 사용자정보(인증서 포함)는 보존 — 기관번호로 확인
        assertThat(decoded.getDataset("gds_userInfo").getString(0, "trainstNo")).isEqualTo("200700110");
    }
}
