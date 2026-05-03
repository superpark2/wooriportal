package com.park.welstory.wooriportal.saramin.service;

import com.park.welstory.wooriportal.saramin.dto.JobPostingDto;
import com.park.welstory.wooriportal.saramin.dto.SearchRequestDto;
import com.park.welstory.wooriportal.saramin.dto.SearchResultDto;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SaraminCrawlerService {

    private static final String BASE_URL = "https://www.saramin.co.kr/zf_user/jobs/list/domestic";

    /** 취소 요청된 jobId 집합 */
    private final Set<String> cancelledJobs = ConcurrentHashMap.newKeySet();

    /** 쿠키 캐시 (5분간 재사용) */
    private Map<String, String> cachedCookies = null;
    private long cookieFetchedAt = 0;
    private static final long COOKIE_TTL_MS = 5 * 60 * 1000;

    private Connection base(String url) {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://www.saramin.co.kr/zf_user/jobs/list/domestic")
                .ignoreContentType(true)
                .timeout(20000);
    }

    // ── URL 빌더 ────────────────────────────────────────────────────────────────
    public String buildSearchUrl(SearchRequestDto req) {
        StringBuilder sb = new StringBuilder(BASE_URL + "?");
        boolean first = true;

        // 위치: loc_cd (구 단위) 또는 loc_mcd (시/도 단위)
        String locCd  = req.getLocCd();
        String locMcd = req.getLocMcd();
        if (locCd != null && !locCd.isBlank())         first = ap(sb, "loc_cd",  locCd,  first);
        else if (locMcd != null && !locMcd.isBlank())  first = ap(sb, "loc_mcd", locMcd, first);

        // 키워드 검색
        String searchWord = req.getSearchWord();
        if (searchWord != null && !searchWord.isBlank()) {
            String encoded = URLEncoder.encode(searchWord, StandardCharsets.UTF_8);
            first = ap(sb, "searchType", req.getSearchType() != null ? req.getSearchType() : "search", first);
            first = ap(sb, "searchword", encoded, first);
        }

        // 직종 대분류 (cat_mcls)
        first = apList(sb, "cat_mcls",     req.getJobMcls(),     first, "%2C");

        // 직종 세부 (cat_kewd)
        first = apList(sb, "cat_kewd",     req.getJobCode(),     first, "%2C");

        // 고용형태 (job_type)
        first = apList(sb, "job_type",     req.getJobType(),     first, "%2C");

        // 기업형태 (company_type)
        first = apList(sb, "company_type", req.getCompanyType(), first, ",");

        // 경력 구분 (exp_cd)
        first = apList(sb, "exp_cd",       req.getExpCd(),       first, "%2C");

        // 경력 년수
        first = ap(sb, "exp_min", req.getExpMin(), first);
        first = ap(sb, "exp_max", req.getExpMax(), first);

        // 학력 상한
        first = ap(sb, "edu_max", req.getEduMax(), first);

        // 급여
        first = ap(sb, "sal_min", req.getSalMin(), first);

        // 정렬
        first = ap(sb, "sort", req.getSort() != null ? req.getSort() : "RL", first);

        // 고정 파라미터
        sb.append("&panel_type=");
        sb.append("&search_optional_item=").append(searchWord != null && !searchWord.isBlank() ? "y" : "n");
        sb.append("&search_done=y");
        sb.append("&panel_count=y");
        sb.append("&preview=y");
        sb.append("&page=").append(req.getPage() <= 0 ? 1 : req.getPage());
        sb.append("&page_count=").append(req.getPageCount() <= 0 ? 25 : req.getPageCount());

        // AJAX 요청 식별 파라미터 (이 값이 있어야 서버가 JSON으로 응답)
        sb.append("&isAjaxRequest=1");
        sb.append("&is_param=1");
        sb.append("&isSectionHome=0");
        sb.append("&type=domestic");

        return sb.toString();
    }

    private boolean ap(StringBuilder sb, String k, String v, boolean first) {
        if (v == null || v.isBlank()) return first;
        if (!first) sb.append("&");
        sb.append(k).append("=").append(v);
        return false;
    }

    private boolean apList(StringBuilder sb, String k, List<String> list, boolean first, String sep) {
        if (list == null || list.isEmpty()) return first;
        if (!first) sb.append("&");
        sb.append(k).append("=").append(String.join(sep, list));
        return false;
    }

    private Map<String, String> fetchCookies() throws IOException {
        long now = System.currentTimeMillis();
        if (cachedCookies != null && (now - cookieFetchedAt) < COOKIE_TTL_MS) {
            log.info("쿠키 캐시 재사용");
            return cachedCookies;
        }
        Connection.Response res = Jsoup.connect("https://www.saramin.co.kr/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .ignoreContentType(true)
                .timeout(20000)
                .execute();
        cachedCookies = res.cookies();
        cookieFetchedAt = now;
        log.info("쿠키 신규 취득: {}", cachedCookies.keySet());
        return cachedCookies;
    }

    /** 취소 요청 */
    public void cancelJob(String jobId) {
        cancelledJobs.add(jobId);
        log.info("취소 요청: jobId={}", jobId);
    }

    /** 취소 상태 정리 */
    private void cleanupJob(String jobId) {
        cancelledJobs.remove(jobId);
    }

    // ── 크롤링 (단일 페이지) ────────────────────────────────────────────────────
    public SearchResultDto crawl(SearchRequestDto req) {
        // page 기본값 보정
        if (req.getPage() <= 0) req.setPage(1);
        if (req.getPageCount() <= 0) req.setPageCount(25);

        String url = buildSearchUrl(req);
        log.info("Crawling page {}: {}", req.getPage(), url);

        try {
            Map<String, String> cookies = fetchCookies();

            Connection.Response response = base(url)
                    .cookies(cookies)
                    .execute();

            String body = response.body();
            log.info("응답 body 앞 200자: {}", body.substring(0, Math.min(200, body.length())));

            JSONObject json = new JSONObject(body);
            String contentsHtml = json.optString("contents", "");
            int totalCount = json.optInt("total_count", 0);

            if (contentsHtml.isBlank()) {
                log.warn("JSON contents 비어있음. 전체 응답: {}", body.substring(0, Math.min(500, body.length())));
                return SearchResultDto.builder()
                        .success(false).crawledUrl(url)
                        .errorMessage("서버가 빈 contents 반환")
                        .postings(new ArrayList<>()).build();
            }

            Document doc = Jsoup.parse(contentsHtml);
            Element listBody = doc.selectFirst("div.list_body");
            if (listBody == null) {
                listBody = doc.body();
                log.info("div.list_body 없음 → body 전체를 listBody로 사용");
            }

            List<JobPostingDto> postings = parse(listBody);

            // 페이지 메타 계산
            int pageCount  = req.getPageCount();
            int totalPages = (int) Math.ceil((double) totalCount / pageCount);

            log.info("파싱 결과: {}건 / 현재 페이지: {}/{} / 총 건수: {}건",
                    postings.size(), req.getPage(), totalPages, totalCount);

            return SearchResultDto.builder()
                    .postings(postings)
                    .crawledUrl(url)
                    .totalCount(totalCount)
                    .currentPage(req.getPage())
                    .pageCount(pageCount)
                    .totalPages(totalPages)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("크롤링 실패: {}", e.getMessage(), e);
            return SearchResultDto.builder()
                    .success(false).crawledUrl(url)
                    .errorMessage("크롤링 실패: " + e.getMessage())
                    .postings(new ArrayList<>()).build();
        }
    }

    /**
     * 전체 페이지 자동 순회 크롤링
     * jobId: 취소 제어용 ID (null이면 취소 불가)
     * maxPages: 최대 수집 페이지 수 (0 이하면 전체)
     */
    public SearchResultDto crawlAll(SearchRequestDto req, int maxPages, String jobId) {
        req.setPage(1);
        if (req.getPageCount() <= 0) req.setPageCount(25);

        List<JobPostingDto> allPostings = new ArrayList<>();
        int totalCount = 0;
        int totalPages = 1;
        String crawledUrl = "";

        Map<String, String> cookies;
        try {
            cookies = fetchCookies();
        } catch (IOException e) {
            log.error("쿠키 취득 실패: {}", e.getMessage());
            return SearchResultDto.builder().success(false)
                    .errorMessage("쿠키 취득 실패: " + e.getMessage())
                    .postings(new ArrayList<>()).build();
        }

        for (int page = 1; ; page++) {
            // 취소 확인
            if (jobId != null && cancelledJobs.contains(jobId)) {
                log.info("jobId={} 취소됨 ({}페이지에서 중단)", jobId, page);
                cleanupJob(jobId);
                break;
            }

            req.setPage(page);
            String url = buildSearchUrl(req);
            crawledUrl = url;

            log.info("[{}] 페이지 {}/{} 크롤링 중...", jobId, page, totalPages);

            try {
                Connection.Response response = base(url).cookies(cookies).execute();
                String body = response.body();

                JSONObject json = new JSONObject(body);
                String contentsHtml = json.optString("contents", "");
                if (page == 1) {
                    totalCount = json.optInt("total_count", 0);
                    totalPages = (int) Math.ceil((double) totalCount / req.getPageCount());
                    if (maxPages > 0) totalPages = Math.min(totalPages, maxPages);
                    log.info("총 {}건 / {}페이지 수집 예정", totalCount, totalPages);
                }

                if (contentsHtml.isBlank()) {
                    log.warn("페이지 {} contents 비어있음, 중단", page);
                    break;
                }

                Document doc = Jsoup.parse(contentsHtml);
                Element listBody = doc.selectFirst("div.list_body");
                if (listBody == null) listBody = doc.body();

                List<JobPostingDto> pagePostings = parse(listBody);
                if (pagePostings.isEmpty()) {
                    log.info("페이지 {} 공고 없음, 수집 종료", page);
                    break;
                }

                allPostings.addAll(pagePostings);
                log.info("페이지 {}: {}건 수집 (누적 {}건)", page, pagePostings.size(), allPostings.size());

                if (page >= totalPages) break;

                // 딜레이 100ms (서버 부하 방지 최소한)
                Thread.sleep(100);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("페이지 {} 크롤링 실패: {}", page, e.getMessage());
                // 실패해도 다음 페이지 시도
                try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }

        if (jobId != null) cleanupJob(jobId);
        log.info("전체 크롤링 완료: 총 {}건", allPostings.size());

        return SearchResultDto.builder()
                .postings(allPostings)
                .crawledUrl(crawledUrl)
                .totalCount(totalCount)
                .currentPage(1)
                .pageCount(req.getPageCount())
                .totalPages(totalPages)
                .success(true)
                .build();
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onPage(int currentPage, int totalPages, int collectedCount);
    }

    /** 진행률 콜백 포함 전체 수집 */
    public SearchResultDto crawlAllWithProgress(SearchRequestDto req, int maxPages, String jobId, ProgressCallback callback) {
        req.setPage(1);
        if (req.getPageCount() <= 0) req.setPageCount(25);

        List<JobPostingDto> allPostings = new ArrayList<>();
        int totalCount = 0;
        int totalPages = 1;
        String crawledUrl = "";

        Map<String, String> cookies;
        try {
            cookies = fetchCookies();
        } catch (IOException e) {
            log.error("쿠키 취득 실패: {}", e.getMessage());
            return SearchResultDto.builder().success(false)
                    .errorMessage("쿠키 취득 실패: " + e.getMessage())
                    .postings(new ArrayList<>()).build();
        }

        for (int page = 1; ; page++) {
            if (jobId != null && cancelledJobs.contains(jobId)) {
                log.info("jobId={} 취소됨 ({}페이지에서 중단)", jobId, page);
                cleanupJob(jobId);
                break;
            }

            req.setPage(page);
            String url = buildSearchUrl(req);
            crawledUrl = url;

            try {
                Connection.Response response = base(url).cookies(cookies).execute();
                String body = response.body();
                JSONObject json = new JSONObject(body);
                String contentsHtml = json.optString("contents", "");

                if (page == 1) {
                    totalCount = json.optInt("total_count", 0);
                    totalPages = (int) Math.ceil((double) totalCount / req.getPageCount());
                    if (maxPages > 0) totalPages = Math.min(totalPages, maxPages);
                    log.info("총 {}건 / {}페이지 수집 예정", totalCount, totalPages);
                }

                if (contentsHtml.isBlank()) { log.warn("페이지 {} 비어있음, 중단", page); break; }

                Document doc = Jsoup.parse(contentsHtml);
                Element listBody = doc.selectFirst("div.list_body");
                if (listBody == null) listBody = doc.body();

                List<JobPostingDto> pagePostings = parse(listBody);
                if (pagePostings.isEmpty()) { log.info("페이지 {} 공고 없음, 종료", page); break; }

                allPostings.addAll(pagePostings);
                log.info("페이지 {}/{}: {}건 (누적 {}건)", page, totalPages, pagePostings.size(), allPostings.size());

                // 진행률 콜백
                if (callback != null) callback.onPage(page, totalPages, allPostings.size());

                if (page >= totalPages) break;

                Thread.sleep(100); // 최소 딜레이

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("페이지 {} 실패: {}", page, e.getMessage());
                try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }

        if (jobId != null) cleanupJob(jobId);
        log.info("수집 완료: {}건", allPostings.size());

        return SearchResultDto.builder()
                .postings(allPostings).crawledUrl(crawledUrl)
                .totalCount(totalCount).currentPage(1)
                .pageCount(req.getPageCount()).totalPages(totalPages)
                .success(true).build();
    }

    /** 하위 호환 오버로드 */
    public SearchResultDto crawlAll(SearchRequestDto req, int maxPages) {
        return crawlAllWithProgress(req, maxPages, null, null);
    }

    // ── 파싱 ────────────────────────────────────────────────────────────────────
    private List<JobPostingDto> parse(Element listBody) {
        if (listBody == null) {
            log.warn("listBody null — 파싱 불가");
            return new ArrayList<>();
        }

        Elements items = listBody.select("div.list_item[id^=rec-]");

        if (items.isEmpty()) {
            log.warn("div.list_item[id^=rec-] 0개. listBody 자식 클래스 확인:");
            listBody.children().stream()
                    .map(Element::className).distinct().limit(10)
                    .forEach(c -> log.warn("  child.class=[{}]", c));
            return new ArrayList<>();
        }

        log.info("공고 카드 개수: {}개", items.size());
        List<JobPostingDto> result = new ArrayList<>();
        for (Element el : items) {
            JobPostingDto dto = extract(el);
            if (dto != null) result.add(dto);
        }
        return result;
    }

    // ── [수정] extract: 지역/경력/학력/고용형태 파싱 오류 수정 ──────────────────
    private JobPostingDto extract(Element el) {

        // ── 제목 + 링크 ─────────────────────────────────────────────────────────
        Element tEl = el.selectFirst(".job_tit a");
        if (tEl == null) tEl = el.selectFirst("a.str_tit");
        if (tEl == null) tEl = el.selectFirst("a[href*='rec_idx']");
        if (tEl == null) return null;

        String title = tEl.text().trim();
        if (title.isBlank()) return null;
        String href = tEl.attr("href");
        String link = href.startsWith("http") ? href : "https://www.saramin.co.kr" + href;

        // ── 회사명 ──────────────────────────────────────────────────────────────
        Element cEl = el.selectFirst(".col.company_nm a.str_tit");
        if (cEl == null) cEl = el.selectFirst(".col.company_nm a");
        if (cEl == null) cEl = el.selectFirst("span.main_corp");
        if (cEl == null) cEl = el.selectFirst(".corp_name a");
        String company = cEl != null ? cEl.text().trim() : "";

        // ── 조건 (지역/경력/학력/고용형태) ──────────────────────────────────────
        // [수정] .job_condition 의 직계 span(> span)만 선택해
        //        .job_sector(키워드 태그), .recruit_btn(스크랩 버튼) 등의 오염을 차단.
        // [수정] 각 span 내 .screen_out 숨김 텍스트를 clone 후 제거하고 텍스트 추출.
        String location   = "";
        String experience = "";
        String education  = "";
        String employType = "";

        Elements condSpans = el.select(".job_condition > span");
        int idx = 0;
        for (Element span : condSpans) {
            // clone 하여 원본 DOM 을 건드리지 않고 screen_out 제거
            Element clone = span.clone();
            clone.select(".screen_out").remove();
            String txt = clone.text().trim();
            if (txt.isBlank()) continue;

            switch (idx++) {
                case 0 -> location   = txt;
                case 1 -> experience = txt;
                case 2 -> education  = txt;
                case 3 -> { employType = txt; }
                default -> { /* 추가 span 무시 */ }
            }
        }

        // job_condition 이 없는 레이아웃 대비 fallback
        if (location.isBlank() && experience.isBlank()) {
            Elements fallback = el.select(".col.notification_info > span").stream()
                    .filter(s -> !s.hasClass("screen_out") && !s.text().isBlank())
                    .collect(java.util.stream.Collectors.toCollection(Elements::new));
            if (fallback.size() > 0) location   = fallback.get(0).text().trim();
            if (fallback.size() > 1) experience = fallback.get(1).text().trim();
            if (fallback.size() > 2) education  = fallback.get(2).text().trim();
            if (fallback.size() > 3) employType = fallback.get(3).text().trim();
        }

        // ── 마감일 ──────────────────────────────────────────────────────────────
        // [수정] .support_detail 안의 .date 우선 → recruit_btn(스크랩버튼) 영역 제외
        Element dEl = el.selectFirst(".support_detail .date");
        if (dEl == null) dEl = el.selectFirst(".col.support_info .date");
        if (dEl == null) dEl = el.selectFirst(".job_date .date");
        if (dEl == null) dEl = el.selectFirst(".job_date span:not(.screen_out)");
        String deadline = dEl != null ? dEl.text().trim() : "";

        // ── 급여 ────────────────────────────────────────────────────────────────
        Element sEl = el.selectFirst(".salary");
        if (sEl == null) sEl = el.selectFirst("[class*='salary']");
        String salary = sEl != null ? sEl.text().trim() : "";

        // ── 뱃지 ────────────────────────────────────────────────────────────────
        // [수정] .badge_area / .area_badge 순으로 탐색, screen_out 제외
        Element bEl = el.selectFirst(".badge_area span:not(.screen_out)");
        if (bEl == null) bEl = el.selectFirst(".area_badge span:not(.screen_out)");
        if (bEl == null) bEl = el.selectFirst("[class*='badge'] span:not(.screen_out)");
        String badge = bEl != null ? bEl.text().trim() : "";

        log.debug("extract → 회사:{} | 지역:{} | 경력:{} | 학력:{} | 고용:{} | 마감:{}",
                company, location, experience, education, employType, deadline);

        return JobPostingDto.builder()
                .title(title).company(company).location(location)
                .experience(experience).education(education)
                .salary(salary).deadline(deadline)
                .employmentType(employType).link(link).badge(badge)
                .build();
    }

    // ── 디버그 ──────────────────────────────────────────────────────────────────
    public String fetchRawHtml() {
        String url = BASE_URL + "?sort=DA&page=1&page_count=5&panel_type=&search_optional_item=n"
                + "&search_done=y&panel_count=y&preview=y&isAjaxRequest=1&is_param=1&type=domestic";
        try {
            Map<String, String> cookies = fetchCookies();
            Connection.Response response = base(url).cookies(cookies).execute();
            return response.body().substring(0, Math.min(5000, response.body().length()));
        } catch (IOException e) {
            return "ERROR: " + e.getMessage();
        }
    }
}