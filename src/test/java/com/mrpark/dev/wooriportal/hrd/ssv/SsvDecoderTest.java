package com.mrpark.dev.wooriportal.hrd.ssv;

import static org.assertj.core.api.Assertions.assertThat;

import com.mrpark.dev.wooriportal.hrd.HrdCaptures;
import org.junit.jupiter.api.Test;

/**
 * 실제 HRD-Net {@code selectDailAtndceDetail.do}(출석부 상세) 응답 캡처로
 * SSV 디코더를 검증한다. 캡처: 2026-06-02, 전산회계1급 자격증(17회차), 수강생 2명.
 */
class SsvDecoderTest {

    @Test
    void decodesCourseAndRoster() {
        SsvData data = SsvDecoder.decode(HrdCaptures.load("/hrd/selectDailAtndceDetail.resp.hex"));

        // 응답 상태 변수
        assertThat(data.getVariable("ErrorCode")).isEqualTo("0");

        // 과정 정보 데이터셋
        SsvDataset course = data.getDataset("ds_dailAtdbTraing");
        assertThat(course).isNotNull();
        assertThat(course.getRowCount()).isEqualTo(1);
        assertThat(course.getString(0, "tracseId")).isEqualTo("AIG20230000443482");
        assertThat(course.getString(0, "tracseNm1")).isEqualTo("전산회계1급 자격증");
        assertThat(course.getString(0, "tracsePd")).isEqualTo("2026-04-02~2026-06-05 (17회차)");
        assertThat(course.getString(0, "tracseTme")).isEqualTo("17");
        assertThat(course.getString(0, "traingTime")).isEqualTo("19:00~22:00(총 3시간)");

        // 출석부(수강생별 출결) 데이터셋
        SsvDataset roster = data.getDataset("ds_dailAtendList");
        assertThat(roster).isNotNull();
        assertThat(roster.getColumnCount()).isEqualTo(48);
        assertThat(roster.getRowCount()).isEqualTo(2);

        assertThat(roster.getString(0, "cstmrNm")).isEqualTo("강건임");
        assertThat(roster.getString(0, "trneeSeNm")).isEqualTo("실업자");
        assertThat(roster.getString(0, "atendSttusNm")).isEqualTo("결석");
        assertThat(roster.getString(0, "atendDe")).isEqualTo("20260602");
        assertThat(roster.getString(0, "maskEncptIhidnm")).isEqualTo("800816-2******");

        assertThat(roster.getString(1, "cstmrNm")).isEqualTo("송유나");
        assertThat(roster.getString(1, "trneeSeNm")).isEqualTo("재직자");
        assertThat(roster.getString(1, "atendSttusNm")).isEqualTo("결석");
        assertThat(roster.getString(1, "maskEncptIhidnm")).isEqualTo("011023-4******");

        // 모든 행은 같은 과정/회차
        assertThat(roster.getString(0, "tracseId")).isEqualTo("AIG20230000443482");
        assertThat(roster.getString(1, "tracseTme")).isEqualTo("17");
    }

    @Test
    void decodesTodayCourseList() {
        SsvData data = SsvDecoder.decode(HrdCaptures.load("/hrd/selectAtendList.resp.hex"));

        assertThat(data.getVariable("ErrorCode")).isEqualTo("0");

        // 당일 과정목록: 73컬럼 x 11과정
        SsvDataset list = data.getDataset("ds_atendInfoList");
        assertThat(list).isNotNull();
        assertThat(list.getColumnCount()).isEqualTo(73);
        assertThat(list.getRowCount()).isEqualTo(11);

        // 상세조회 키가 모든 과정에 채워져 있어야 한다(폴러가 이걸로 detail 호출)
        for (int i = 0; i < list.getRowCount(); i++) {
            assertThat(list.getString(i, "tracseId")).as("row %d tracseId", i).isNotBlank();
            assertThat(list.getString(i, "tracseTme")).as("row %d tracseTme", i).isNotBlank();
            assertThat(list.getString(i, "tracseNm")).as("row %d tracseNm", i).isNotBlank();
        }

        // 마지막 detail 캡처(전산회계1급 17회차)와 동일 과정이 목록에 존재
        boolean found = false;
        for (int i = 0; i < list.getRowCount(); i++) {
            if ("AIG20230000443482".equals(list.getString(i, "tracseId"))
                    && "17".equals(list.getString(i, "tracseTme"))) {
                found = true;
                assertThat(list.getString(i, "tracseNm")).isEqualTo("전산회계1급 자격증");
                assertThat(list.getString(i, "oprtnInsttNm")).isEqualTo("부천고용센터");
            }
        }
        assertThat(found).as("전산회계1급 17회차가 당일목록에 존재").isTrue();
    }
}
