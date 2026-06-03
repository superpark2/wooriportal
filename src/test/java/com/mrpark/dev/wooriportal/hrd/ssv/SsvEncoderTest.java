package com.mrpark.dev.wooriportal.hrd.ssv;

import static org.assertj.core.api.Assertions.assertThat;

import com.mrpark.dev.wooriportal.hrd.HrdCaptures;
import org.junit.jupiter.api.Test;

/**
 * 인코더 검증: 캡처를 디코드한 뒤 다시 인코딩하면 원본(압축 해제본)과 바이트 단위로 일치해야 한다.
 * 이게 보장돼야 실서버에 잘못된 요청을 쏘지 않는다.
 */
class SsvEncoderTest {

    private static void assertRoundTrip(String resource) {
        byte[] body = HrdCaptures.load(resource);
        byte[] inflatedOriginal = SsvDecoder.inflate(body);

        SsvData decoded = SsvDecoder.decode(body);
        byte[] reEncoded = SsvEncoder.encodeInflated(decoded);

        assertThat(reEncoded)
                .as("re-encoded %s must equal original inflated bytes", resource)
                .isEqualTo(inflatedOriginal);
    }

    @Test
    void roundTripsDetailResponse() {
        assertRoundTrip("/hrd/selectDailAtndceDetail.resp.hex");
    }

    @Test
    void roundTripsCourseListResponse() {
        assertRoundTrip("/hrd/selectAtendList.resp.hex");
    }

    @Test
    void roundTripsDetailRequest() {
        // 인증서·__DS_TRANS_INFO__(트레일링 null 포함)·gds_userInfo 까지 포함한 요청
        assertRoundTrip("/hrd/selectDailAtndceDetail.req.hex");
    }

    @Test
    void modifyingValuesReEncodesAndReDecodes() {
        SsvData data = SsvDecoder.decode(HrdCaptures.load("/hrd/selectDailAtndceDetail.req.hex"));

        // 세션/타임스탬프/조회키 교체 (실제 요청 빌드 시나리오)
        data.setVariable("JSESSIONID", "NEWSESSION!-111!-222");
        data.setVariable("request-timestamp", "20260603093000");
        SsvDataset cond = data.getDataset("ds_cond");
        cond.setString(0, "tracseId", "AIG20250000541286");
        cond.setString(0, "tracseTme", "1");
        cond.setString(0, "traingDe", "20260603");

        // 인코딩 → 다시 디코딩 → 값이 반영됐는지
        byte[] reInflated = SsvEncoder.encodeInflated(data);
        SsvData again = SsvDecoder.decodeInflated(reInflated);

        assertThat(again.getVariable("JSESSIONID")).isEqualTo("NEWSESSION!-111!-222");
        assertThat(again.getVariable("request-timestamp")).isEqualTo("20260603093000");
        SsvDataset cond2 = again.getDataset("ds_cond");
        assertThat(cond2.getString(0, "tracseId")).isEqualTo("AIG20250000541286");
        assertThat(cond2.getString(0, "tracseTme")).isEqualTo("1");
        assertThat(cond2.getString(0, "traingDe")).isEqualTo("20260603");

        // 사용자 정보(인증서 등)는 그대로 보존
        assertThat(again.getDataset("gds_userInfo")).isNotNull();
        assertThat(again.getDataset("gds_userInfo").getString(0, "trainstNo")).isEqualTo("200700110");
    }

    @Test
    void framedOutputDecodesBack() {
        // FF AD + deflate 전체 프레이밍도 왕복되는지
        SsvData data = SsvDecoder.decode(HrdCaptures.load("/hrd/selectDailAtndceDetail.resp.hex"));
        byte[] framed = SsvEncoder.encode(data);
        assertThat(framed[0] & 0xFF).isEqualTo(0xFF);
        assertThat(framed[1] & 0xFF).isEqualTo(0xAD);

        SsvData reDecoded = SsvDecoder.decode(framed);
        assertThat(reDecoded.getDataset("ds_dailAtendList").getString(1, "cstmrNm")).isEqualTo("송유나");
    }
}
