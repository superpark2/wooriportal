package com.mrpark.dev.wooriportal.hrd;

import static org.assertj.core.api.Assertions.assertThat;

import com.mrpark.dev.wooriportal.hrd.dto.HrdAttendee;
import com.mrpark.dev.wooriportal.hrd.dto.HrdCourse;
import com.mrpark.dev.wooriportal.hrd.dto.HrdCourseDetail;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvData;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvDecoder;
import java.util.List;
import org.junit.jupiter.api.Test;

class HrdAttendanceMapperTest {

    private static SsvData decode(String resource) {
        return SsvDecoder.decode(HrdCaptures.load(resource));
    }

    @Test
    void mapsTodayCourses() {
        List<HrdCourse> courses = HrdAttendanceMapper.toCourses(decode("/hrd/selectAtendList.resp.hex"));

        assertThat(courses).hasSize(11);

        HrdCourse jeonsan = courses.stream()
                .filter(c -> "AIG20230000443482".equals(c.getTracseId()) && "17".equals(c.getTracseTme()))
                .findFirst().orElseThrow();
        assertThat(jeonsan.getTracseNm()).isEqualTo("전산회계1급 자격증");
        assertThat(jeonsan.getMainTracseNm()).isEqualTo("근로자");
        assertThat(jeonsan.getOprtnInsttNm()).isEqualTo("부천고용센터");
        assertThat(jeonsan.getStartDateDisplay()).isEqualTo("2026-04-02");
        assertThat(jeonsan.getEndDateDisplay()).isEqualTo("2026-06-05");
        assertThat(jeonsan.getCrseTracseSe()).isEqualTo("C0061");
        assertThat(jeonsan.getQrCharstCn()).contains("TRACSE_ID=AIG20230000443482", "TRACSE_TME=17");

        // 상세조회 키는 모든 과정에 존재
        assertThat(courses).allSatisfy(c -> {
            assertThat(c.getTracseId()).isNotBlank();
            assertThat(c.getTracseTme()).isNotBlank();
            assertThat(c.getTracseNm()).isNotBlank();
        });
    }

    @Test
    void mapsCourseDetailAndRoster() {
        SsvData data = decode("/hrd/selectDailAtndceDetail.resp.hex");

        HrdCourseDetail detail = HrdAttendanceMapper.toCourseDetail(data);
        assertThat(detail).isNotNull();
        assertThat(detail.getTracseNm()).isEqualTo("전산회계1급 자격증");
        assertThat(detail.getTracsePd()).isEqualTo("2026-04-02~2026-06-05 (17회차)");
        assertThat(detail.getTraingTime()).isEqualTo("19:00~22:00(총 3시간)");

        List<HrdAttendee> roster = HrdAttendanceMapper.toRoster(data);
        assertThat(roster).hasSize(2);
        assertThat(roster).extracting(HrdAttendee::getCstmrNm).containsExactly("강건임", "송유나");
        assertThat(roster).extracting(HrdAttendee::getAtendSttusNm).containsOnly("결석");
        assertThat(roster.get(0).getTrneeSeNm()).isEqualTo("실업자");
        assertThat(roster.get(1).getMaskEncptIhidnm()).isEqualTo("011023-4******");
    }
}
