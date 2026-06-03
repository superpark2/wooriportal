package com.mrpark.dev.wooriportal.hrd;

import com.mrpark.dev.wooriportal.hrd.dto.HrdDailyAttendance;
import com.mrpark.dev.wooriportal.hrd.session.HrdSession;
import com.mrpark.dev.wooriportal.hrd.session.HrdSessionStore;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvData;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvDataset;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvDecoder;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HRD-Net 행정시스템을 직접 호출하는 클라이언트.
 *
 * <p>캡처한 요청을 템플릿으로 받아 살아있는 세션({@link HrdSessionStore})·타임스탬프·
 * 조회키(ds_cond)만 갈아끼워 POST 한다. 응답은 SSV 로 디코딩해 DTO 로 돌려준다.</p>
 *
 * <p>요청 템플릿(gds_userInfo·인증서 포함)은 PII 라 코드에 박지 않고, 프록시 하베스터가
 * 수확하거나 운영자가 외부 파일로 공급한 바이트를 받아 쓴다.</p>
 */
@Component
public class HrdNetClient {

    private static final String BASE = "https://www.hrd.go.kr:44381";
    static final String DETAIL_URL = BASE + "/hrdg/zz/hzzd/hzzdi/selectDailAtndceDetail.do";
    private static final String DETAIL_REFERER = BASE + "/HRDNUI/HZZ/DX/HZZDAX0103P.xfdl";
    private static final String USER_AGENT =
            "XPLATFORM/9.2.2 Runtmie (compatible; Mozilla/4.0; MSIE7.0; System=Win64; Device=; OS=Windows 10; Screen=1920*1080*16M)";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final HrdSessionStore sessionStore;
    private final RestTemplate restTemplate;

    public HrdNetClient(HrdSessionStore sessionStore) {
        this.sessionStore = sessionStore;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 출석부 상세 요청 본문을 만든다(순수 함수, 네트워크 없음).
     *
     * @param templateBody 캡처한 selectDailAtndceDetail 요청 본문(FF AD + zlib)
     * @return 전송용 요청 본문(FF AD + zlib)
     */
    public byte[] buildDetailRequestBody(byte[] templateBody, HrdSession session,
                                         String tracseId, String tracseTme, String traingDe) {
        SsvData req = SsvDecoder.decode(templateBody);
        if (session != null) {
            req.setVariable("JSESSIONID", session.getJsessionId());
            if (session.getWmonid() != null && !session.getWmonid().isBlank()) {
                req.setVariable("WMONID", session.getWmonid());
            }
        }
        req.setVariable("request-timestamp", LocalDateTime.now().format(TS));

        SsvDataset cond = req.getDataset("ds_cond");
        if (cond != null && cond.getRowCount() > 0) {
            cond.setString(0, "tracseId", tracseId);
            cond.setString(0, "tracseTme", tracseTme);
            cond.setString(0, "traingDe", traingDe);
        }
        return SsvEncoder.encode(req);
    }

    /**
     * 한 과정의 당일 출결을 실서버에서 직접 조회한다.
     *
     * @throws HrdSessionExpiredException 세션이 없거나 응답이 SSV 가 아닐 때
     */
    public HrdDailyAttendance fetchDailyAttendance(byte[] templateBody,
                                                   String tracseId, String tracseTme, String traingDe) {
        SsvData data = fetchDetailRaw(templateBody, tracseId, tracseTme, traingDe);
        return new HrdDailyAttendance(
                HrdAttendanceMapper.toCourseDetail(data),
                HrdAttendanceMapper.toRoster(data));
    }

    /**
     * 상세 응답을 디코딩만 해서 원시 {@link SsvData} 로 반환.
     *
     * <p>세션은 <b>선택</b>이다 — 실인증은 요청 템플릿의 인증서(gds_userInfo)가 하므로,
     * 살아있는 세션이 있으면 그 쿠키를, 없으면 템플릿에 박힌 값을 그대로 쓴다.</p>
     */
    public SsvData fetchDetailRaw(byte[] templateBody, String tracseId, String tracseTme, String traingDe) {
        HrdSession session = sessionStore.current().orElse(null);

        SsvData req = SsvDecoder.decode(templateBody);
        String jsession;
        String wmonid;
        if (session != null) {
            jsession = session.getJsessionId();
            wmonid = session.getWmonid();
            req.setVariable("JSESSIONID", jsession);
            if (wmonid != null && !wmonid.isBlank()) {
                req.setVariable("WMONID", wmonid);
            }
        } else {
            jsession = req.getVariable("JSESSIONID");   // 템플릿 값 사용
            wmonid = req.getVariable("WMONID");
        }
        req.setVariable("request-timestamp", LocalDateTime.now().format(TS));
        SsvDataset cond = req.getDataset("ds_cond");
        if (cond != null && cond.getRowCount() > 0) {
            cond.setString(0, "tracseId", tracseId);
            cond.setString(0, "tracseTme", tracseTme);
            cond.setString(0, "traingDe", traingDe);
        }

        ResponseEntity<byte[]> resp = post(DETAIL_URL, DETAIL_REFERER, SsvEncoder.encode(req), jsession, wmonid);
        byte[] respBody = resp.getBody();
        if (!isSsv(respBody)) {
            throw new HrdSessionExpiredException("HRD 응답이 SSV 가 아님(로그인 리다이렉트/인증 실패 추정)");
        }
        return SsvDecoder.decode(respBody);
    }

    private ResponseEntity<byte[]> post(String url, String referer, byte[] body, String jsession, String wmonid) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/octet-stream");
        headers.set("Accept", "*/*");
        headers.set("Cache-Control", "no-cache");
        headers.set("Cookie", cookieHeader(jsession, wmonid));
        headers.set("Referer", referer);
        headers.set("User-Agent", USER_AGENT);
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), byte[].class);
    }

    private static String cookieHeader(String jsession, String wmonid) {
        StringBuilder sb = new StringBuilder("gv_ssoFlag=; ");
        if (wmonid != null && !wmonid.isBlank()) {
            sb.append("WMONID=").append(wmonid).append("; ");
        }
        if (jsession != null && !jsession.isBlank()) {
            sb.append("JSESSIONID=").append(jsession).append(";");
        }
        return sb.toString();
    }

    private static boolean isSsv(byte[] body) {
        return body != null && body.length >= 2 && (body[0] & 0xFF) == 0xFF && (body[1] & 0xFF) == 0xAD;
    }
}
