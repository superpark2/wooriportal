package com.mrpark.dev.wooriportal.jobinfo.jobkorea.service;

import com.mrpark.dev.wooriportal.jobinfo.jobkorea.dto.JkPosting;
import com.mrpark.dev.wooriportal.jobinfo.jobkorea.dto.JkRequest;
import com.mrpark.dev.wooriportal.jobinfo.jobkorea.dto.JkResult;
import lombok.extern.slf4j.Slf4j;
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
 * 잡코리아 채용정보 크롤러.
 * /Search 페이지는 서버사이드 렌더링이며 페이지당 20건 고정, Page_No 로 페이징한다.
 * 카드 앵커는 data-sentry-component 속성(CardJob/Title 등)이 안정적이다.
 */
@Slf4j
@Service
public class JobKoreaCrawlerService {

    private static final String HOST     = "https://www.jobkorea.co.kr";
    private static final String BASE_URL = HOST + "/Search";

    /** 페이지당 결과 수 (잡코리아 고정값) */
    private static final int PAGE_SIZE = 20;

    /** 총 건수 추출: "44,890건" (천단위 콤마, 3자리 이상). 첫 매칭이 검색결과 총계. */
    private static final Pattern TOTAL_PATTERN =
            Pattern.compile("([0-9,]{3,})\\s*건");
    private static final Pattern DEADLINE_PATTERN =
            Pattern.compile("D-\\d+|오늘마감|내일마감|상시채용|상시|~.*까지|\\d{1,2}/\\d{1,2}\\s*\\(.\\)");

    private final Set<String> cancelledJobs = ConcurrentHashMap.newKeySet();

    // ── URL 빌더 ────────────────────────────────────────────────────────────────
    public String buildSearchUrl(JkRequest req) {
        StringBuilder sb = new StringBuilder(BASE_URL + "?");
        sb.append("Page_No=").append(req.getPage() <= 0 ? 1 : req.getPage());
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            sb.append("&stext=").append(URLEncoder.encode(req.getKeyword(), StandardCharsets.UTF_8));
        }
        if (req.getCareerType() != null && !req.getCareerType().isBlank()) {
            sb.append("&careerType=").append(req.getCareerType());
        }
        if (req.getLocation() != null && !req.getLocation().isEmpty()) {
            sb.append("&loc=").append(String.join(",", req.getLocation()));
        }
        if (req.getEmpType() != null && !req.getEmpType().isEmpty()) {
            sb.append("&empType=").append(String.join(",", req.getEmpType()));
        }
        if (req.getEduLevel() != null && !req.getEduLevel().isBlank()) {
            sb.append("&eduLevel=").append(req.getEduLevel());
        }
        if (req.getJobTypes() != null && !req.getJobTypes().isEmpty()) {
            for (String t : req.getJobTypes()) {
                sb.append("&indType=").append(t);
            }
        }
        return sb.toString();
    }

    private Connection base(String url) {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .header("Connection", "keep-alive")
                .header("Referer", HOST + "/")
                .ignoreContentType(true)
                .maxBodySize(0)
                .timeout(20000);
    }

    public void cancelJob(String jobId) { cancelledJobs.add(jobId); }
    public boolean isCancelled(String jobId) { return jobId != null && cancelledJobs.contains(jobId); }
    public void cleanupJobPublic(String jobId) { if (jobId != null) cancelledJobs.remove(jobId); }

    // ── 단일 표시 페이지 (내부 20건 페이지를 모아 pageCount 만큼 슬라이스) ───────────────
    public JkResult crawl(JkRequest req) {
        int displayPage = req.getPage() <= 0 ? 1 : req.getPage();
        int size = req.getPageCount() <= 0 ? PAGE_SIZE : req.getPageCount();

        int startIdx = (displayPage - 1) * size;
        int firstUnderlying = startIdx / PAGE_SIZE + 1;
        int offset = startIdx % PAGE_SIZE;
        String firstUrl = "";

        try {
            List<JkPosting> buffer = new ArrayList<>();
            int totalCount = 0;
            for (int u = firstUnderlying; buffer.size() < offset + size; u++) {
                req.setPage(u);
                String url = buildSearchUrl(req);
                if (u == firstUnderlying) firstUrl = url;
                Document doc = base(url).get();
                if (u == firstUnderlying) totalCount = extractTotalCount(doc);

                List<JkPosting> pagePostings = parse(doc);
                if (pagePostings.isEmpty()) break;
                buffer.addAll(pagePostings);

                int totalUnderlying = (int) Math.ceil((double) totalCount / PAGE_SIZE);
                if (u >= totalUnderlying) break;
            }
            req.setPage(displayPage);

            List<JkPosting> postings = offset >= buffer.size()
                    ? new ArrayList<>()
                    : new ArrayList<>(buffer.subList(offset, Math.min(offset + size, buffer.size())));

            int totalPages = (int) Math.ceil((double) totalCount / size);

            return JkResult.builder()
                    .postings(postings).crawledUrl(firstUrl)
                    .totalCount(totalCount).currentPage(displayPage)
                    .pageCount(size).totalPages(totalPages)
                    .success(true).build();
        } catch (Exception e) {
            log.error("잡코리아 크롤링 실패: {}", e.getMessage(), e);
            return JkResult.builder()
                    .success(false).crawledUrl(firstUrl)
                    .errorMessage("크롤링 실패: " + e.getMessage())
                    .postings(new ArrayList<>()).build();
        }
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onPage(int currentPage, int totalPages, int collectedCount);
    }

    /** 진행률 콜백 포함 전체 수집 (내부 20건 페이지 단위) */
    public JkResult crawlAllWithProgress(JkRequest req, int maxPages, String jobId, ProgressCallback callback) {
        List<JkPosting> allPostings = new ArrayList<>();
        int totalCount = 0;
        int totalPages = 1;
        String crawledUrl = "";

        for (int page = 1; ; page++) {
            if (jobId != null && cancelledJobs.contains(jobId)) break;

            req.setPage(page);
            String url = buildSearchUrl(req);
            crawledUrl = url;
            try {
                Document doc = base(url).get();
                if (page == 1) {
                    totalCount = extractTotalCount(doc);
                    totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
                    if (maxPages > 0) totalPages = Math.min(totalPages, maxPages);
                }
                List<JkPosting> pagePostings = parse(doc);
                if (pagePostings.isEmpty()) {
                    log.info("잡코리아 페이지 {} 공고 없음, 종료", page);
                    break;
                }
                allPostings.addAll(pagePostings);
                if (callback != null) callback.onPage(page, totalPages, allPostings.size());
                if (page >= totalPages) break;
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("잡코리아 페이지 {} 실패: {}", page, e.getMessage());
                try { Thread.sleep(400); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }

        return JkResult.builder()
                .postings(allPostings).crawledUrl(crawledUrl)
                .totalCount(totalCount).currentPage(1)
                .pageCount(PAGE_SIZE).totalPages(totalPages)
                .success(true).build();
    }

    // ── 파싱 ────────────────────────────────────────────────────────────────────
    private int extractTotalCount(Document doc) {
        Matcher m = TOTAL_PATTERN.matcher(doc.text());
        return m.find() ? Integer.parseInt(m.group(1).replace(",", "")) : 0;
    }

    private List<JkPosting> parse(Document doc) {
        Elements cards = doc.select("[data-sentry-component=CardJob]");
        List<JkPosting> result = new ArrayList<>();
        for (Element card : cards) {
            JkPosting dto = extract(card);
            if (dto != null) result.add(dto);
        }
        return result;
    }

    private JkPosting extract(Element card) {
        Element titleEl = card.selectFirst("a[data-sentry-component=Title]");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isBlank()) return null;
        String href = titleEl.attr("href");
        String link = href.startsWith("http") ? href : HOST + href;

        Element companyEl = card.selectFirst("span.text-typo-b2-16");
        String company = companyEl != null ? companyEl.text().trim() : "";

        Element badgeEl = card.selectFirst("span.text-pink");
        String badge = badgeEl != null ? badgeEl.text().trim() : "";

        // 회색 칩(b4-14): [0] 지역, [1] 업종·직무
        Elements chips = card.select("span.text-typo-b4-14");
        String location    = chips.size() > 0 ? chips.get(0).text().trim() : "";
        String jobCategory = chips.size() > 1 ? chips.get(1).text().trim() : "";

        // 경력 정보 (회색 c1-13)
        Element expEl = card.selectFirst("span.text-gray700.text-typo-c1-13");
        String experience = expEl != null ? expEl.text().trim() : "";

        // 마감일 — 목록엔 대부분 없음. D-day/상시 패턴 탐색
        String deadline = "";
        for (Element s : card.select("span")) {
            String t = s.text().trim();
            if (t.length() <= 20 && DEADLINE_PATTERN.matcher(t).find()) { deadline = t; break; }
        }

        return JkPosting.builder()
                .title(title).company(company).location(location)
                .jobCategory(jobCategory).experience(experience)
                .badge(badge).deadline(deadline).link(link)
                .build();
    }
}
