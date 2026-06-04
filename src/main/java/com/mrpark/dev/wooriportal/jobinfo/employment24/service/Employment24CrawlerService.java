package com.mrpark.dev.wooriportal.jobinfo.employment24.service;

import com.mrpark.dev.wooriportal.jobinfo.employment24.dto.Emp24JobCategory;
import com.mrpark.dev.wooriportal.jobinfo.employment24.dto.Emp24Posting;
import com.mrpark.dev.wooriportal.jobinfo.employment24.dto.Emp24Request;
import com.mrpark.dev.wooriportal.jobinfo.employment24.dto.Emp24Result;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 고용24(워크넷) 채용정보 크롤러.
 * list 엔드포인트는 서버사이드 렌더링이며 페이지당 10건 고정, pageIndex 로 페이징한다.
 */
@Slf4j
@Service
public class Employment24CrawlerService {

    private static final String HOST     = "https://www.work24.go.kr";
    private static final String BASE_URL = HOST + "/wk/a/b/1200/retriveDtlEmpSrchList.do";
    private static final String JOBS_URL = HOST + "/wk/l/b/1100/selectJobsSubSearchList.do";

    /** 페이지당 결과 수 (work24 고정값) */
    private static final int PAGE_SIZE = 10;

    private static final Pattern TOTAL_PATTERN =
            Pattern.compile("totalRecordCount\\s*:\\s*([0-9]+)");

    /** 취소 요청된 jobId 집합 */
    private final Set<String> cancelledJobs = ConcurrentHashMap.newKeySet();

    // ── URL 빌더 ────────────────────────────────────────────────────────────────
    public String buildSearchUrl(Emp24Request req) {
        StringBuilder sb = new StringBuilder(BASE_URL + "?");
        sb.append("pageIndex=").append(req.getPage() <= 0 ? 1 : req.getPage());

        // work24 제목/내용 검색어 파라미터는 srcKeyword (keyword 는 무시됨)
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            sb.append("&srcKeyword=").append(URLEncoder.encode(req.getKeyword(), StandardCharsets.UTF_8));
        }
        // 지역: 00000(전국)은 미전송
        String region = req.getRegion();
        if (region != null && !region.isBlank() && !"00000".equals(region)) {
            sb.append("&region=").append(region);
        }

        ap(sb, "occupation",    req.getOccupation());
        ap(sb, "careerTypes",   req.getCareerTypes());
        ap(sb, "academicGbn",   req.getAcademicGbn());
        ap(sb, "empTpGbcd",     req.getEmpTpGbcd());
        ap(sb, "holidayGbn",    req.getHolidayGbn());
        ap(sb, "enterPriseGbn", req.getEnterPriseGbn());
        ap(sb, "employGbn",     req.getEmployGbn());

        // 임금: payGbn 선택 시에만 min/max 전송
        if (req.getPayGbn() != null && !req.getPayGbn().isBlank()) {
            sb.append("&payGbn=").append(req.getPayGbn());
            if (req.getMinPay() != null && !req.getMinPay().isBlank()) sb.append("&minPay=").append(req.getMinPay());
            if (req.getMaxPay() != null && !req.getMaxPay().isBlank()) sb.append("&maxPay=").append(req.getMaxPay());
        }
        return sb.toString();
    }

    // ── 직종(직업분류) 트리 ──────────────────────────────────────────────────────
    /**
     * 직종 분류 목록 조회. superJobsCd 가 비면 대분류(13개), 값이 있으면 해당 분류의 하위 목록.
     * work24 selectJobsSubSearchList.do JSON 응답을 변환한다.
     */
    public List<Emp24JobCategory> fetchJobCategories(String superJobsCd) {
        String sup = superJobsCd == null ? "" : superJobsCd.trim();
        String url = JOBS_URL + "?type=worknet&superJobsCd=" + URLEncoder.encode(sup, StandardCharsets.UTF_8);
        List<Emp24JobCategory> result = new ArrayList<>();
        try {
            // Accept를 JSON으로 지정하지 않으면 work24가 XML(ModelMap)을 반환한다.
            String body = base(url)
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .ignoreContentType(true).execute().body();
            JSONArray arr = new JSONObject(body).optJSONArray("result");
            if (arr == null) return result;
            // 대분류(superJobsCd 비었을 때) 외에는 하위가 있을 수 있으나, 본 UI는 대→중 2단계만 사용한다.
            boolean topLevel = sup.isEmpty();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String code = o.optString("jobsCd", "");
                String name = o.optString("jobsCdKorNm", "");
                if (code.isBlank() || name.isBlank()) continue;
                result.add(new Emp24JobCategory(code, name, topLevel));
            }
        } catch (Exception e) {
            log.error("직종 분류 조회 실패(superJobsCd={}): {}", sup, e.getMessage());
        }
        return result;
    }

    /** 리스트 파라미터를 콤마로 연결해 추가 (work24는 콤마 다중값 지원) */
    private void ap(StringBuilder sb, String key, List<String> values) {
        if (values == null || values.isEmpty()) return;
        List<String> clean = new ArrayList<>();
        for (String v : values) if (v != null && !v.isBlank()) clean.add(v);
        if (clean.isEmpty()) return;
        sb.append("&").append(key).append("=").append(String.join(",", clean));
    }

    private Connection base(String url) {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .header("Connection", "keep-alive")
                .header("Referer", BASE_URL)
                .ignoreContentType(true)
                .maxBodySize(0)
                .timeout(20000);
    }

    /** 취소 요청 */
    public void cancelJob(String jobId) {
        cancelledJobs.add(jobId);
    }

    /** 취소 여부 조회 */
    public boolean isCancelled(String jobId) {
        return jobId != null && cancelledJobs.contains(jobId);
    }

    /** 작업 종료 후 취소 플래그 정리 (컨트롤러 finally 에서 호출) */
    public void cleanupJobPublic(String jobId) {
        if (jobId != null) cancelledJobs.remove(jobId);
    }

    // ── 단일 페이지 ──────────────────────────────────────────────────────────────
    public Emp24Result crawl(Emp24Request req) {
        int displayPage = req.getPage() <= 0 ? 1 : req.getPage();
        int size = req.getPageCount() <= 0 ? 25 : req.getPageCount();

        // 표시 페이지 size건을 채우기 위해 필요한 work24 내부 페이지(10건 단위) 범위 계산
        int startIdx = (displayPage - 1) * size;       // 0-based 전체 인덱스
        int firstUnderlying = startIdx / PAGE_SIZE + 1; // work24 pageIndex (1-based)
        int offset = startIdx % PAGE_SIZE;              // 첫 내부 페이지 내 시작 오프셋
        String firstUrl = "";

        try {
            List<Emp24Posting> buffer = new ArrayList<>();
            int totalCount = 0;
            for (int u = firstUnderlying; buffer.size() < offset + size; u++) {
                req.setPage(u);
                String url = buildSearchUrl(req);
                if (u == firstUnderlying) firstUrl = url;
                Document doc = base(url).get();
                if (u == firstUnderlying) totalCount = extractTotalCount(doc.html());

                List<Emp24Posting> pagePostings = parse(doc);
                if (pagePostings.isEmpty()) break;
                buffer.addAll(pagePostings);

                int totalUnderlying = (int) Math.ceil((double) totalCount / PAGE_SIZE);
                if (u >= totalUnderlying) break;
            }
            req.setPage(displayPage);

            // 버퍼에서 표시 페이지에 해당하는 size건만 슬라이스
            List<Emp24Posting> postings = offset >= buffer.size()
                    ? new ArrayList<>()
                    : new ArrayList<>(buffer.subList(offset, Math.min(offset + size, buffer.size())));

            int totalPages = (int) Math.ceil((double) totalCount / size);

            return Emp24Result.builder()
                    .postings(postings)
                    .crawledUrl(firstUrl)
                    .totalCount(totalCount)
                    .currentPage(displayPage)
                    .pageCount(size)
                    .totalPages(totalPages)
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("고용24 크롤링 실패: {}", e.getMessage(), e);
            return Emp24Result.builder()
                    .success(false).crawledUrl(firstUrl)
                    .errorMessage("크롤링 실패: " + e.getMessage())
                    .postings(new ArrayList<>()).build();
        }
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onPage(int currentPage, int totalPages, int collectedCount);
    }

    /** 진행률 콜백 포함 전체 수집 */
    public Emp24Result crawlAllWithProgress(Emp24Request req, int maxPages, String jobId, ProgressCallback callback) {
        List<Emp24Posting> allPostings = new ArrayList<>();
        int totalCount = 0;
        int totalPages = 1;
        String crawledUrl = "";

        for (int page = 1; ; page++) {
            if (jobId != null && cancelledJobs.contains(jobId)) {
                break; // 취소 플래그 정리는 컨트롤러 finally(cleanupJobPublic)에서
            }

            req.setPage(page);
            String url = buildSearchUrl(req);
            crawledUrl = url;

            try {
                Document doc = base(url).get();

                if (page == 1) {
                    totalCount = extractTotalCount(doc.html());
                    totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
                    if (maxPages > 0) totalPages = Math.min(totalPages, maxPages);
                }

                List<Emp24Posting> pagePostings = parse(doc);
                if (pagePostings.isEmpty()) {
                    log.info("고용24 페이지 {} 공고 없음, 수집 종료", page);
                    break;
                }

                allPostings.addAll(pagePostings);
                if (callback != null) callback.onPage(page, totalPages, allPostings.size());

                if (page >= totalPages) break;

                Thread.sleep(150);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("고용24 페이지 {} 실패: {}", page, e.getMessage());
                try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }

        return Emp24Result.builder()
                .postings(allPostings).crawledUrl(crawledUrl)
                .totalCount(totalCount).currentPage(1)
                .pageCount(PAGE_SIZE).totalPages(totalPages)
                .success(true).build();
    }

    // ── 파싱 ────────────────────────────────────────────────────────────────────
    private int extractTotalCount(String html) {
        Matcher m = TOTAL_PATTERN.matcher(html);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private List<Emp24Posting> parse(Document doc) {
        Elements rows = doc.select("tr[id^=list]");
        List<Emp24Posting> result = new ArrayList<>();
        for (Element row : rows) {
            Emp24Posting dto = extract(row);
            if (dto != null) result.add(dto);
        }
        return result;
    }

    private Emp24Posting extract(Element row) {
        // 제목 + 링크
        Element titleEl = row.selectFirst("a[data-emp-detail]");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isBlank()) return null;
        String href = titleEl.attr("href");
        String link = href.startsWith("http") ? href : HOST + href;

        // 회사명
        Element companyEl = row.selectFirst("a.cp_name");
        String company = companyEl != null ? companyEl.text().trim() : "";

        // 급여 (li.dollar)
        Element salaryEl = row.selectFirst("li.dollar span.item");
        String salary = salaryEl != null ? salaryEl.text().replaceAll("\\s+", " ").trim() : "";

        // 경력 / 학력 (li.member 안의 span.item 2개)
        Elements memberItems = row.select("li.member span.item");
        String career    = memberItems.size() > 0 ? memberItems.get(0).text().trim() : "";
        String education = memberItems.size() > 1 ? memberItems.get(1).text().trim() : "";

        // 고용형태/근무일 (li.time)
        Element timeEl = row.selectFirst("li.time span.item");
        String employmentType = timeEl != null ? timeEl.text().replaceAll("\\s+", " ").trim() : "";

        // 지역 (li.site)
        Element siteEl = row.selectFirst("li.site p");
        String location = siteEl != null ? siteEl.text().replaceAll("\\s+", " ").trim() : "";

        // 마감일 / 등록일 (td 우측의 p.s1_r)
        String deadline = "";
        String registeredDate = "";
        for (Element p : row.select("p.s1_r")) {
            String t = p.text().trim();
            if (t.startsWith("마감일")) deadline = t.replace("마감일 :", "").trim();
            else if (t.startsWith("등록일")) registeredDate = t.replace("등록일 :", "").trim();
        }

        return Emp24Posting.builder()
                .title(title).company(company).location(location)
                .career(career).education(education)
                .employmentType(employmentType).salary(salary)
                .deadline(deadline).registeredDate(registeredDate)
                .link(link).build();
    }
}
