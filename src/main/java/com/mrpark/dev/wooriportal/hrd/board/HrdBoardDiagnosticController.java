package com.mrpark.dev.wooriportal.hrd.board;

import com.mrpark.dev.wooriportal.hrd.HrdNetClient;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvData;
import com.mrpark.dev.wooriportal.hrd.ssv.SsvDataset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전광판 진단(머신/로그인 없이 폴 결과 확인). {@code /coolapi/**} 라 permitAll.
 */
@RestController
@RequestMapping("/coolapi/hrd/board")
@RequiredArgsConstructor
public class HrdBoardDiagnosticController {

    /** 출결 시각/상태 관련 후보 컬럼(어디에 입실/퇴실이 들어오는지 확인용). */
    private static final String[] DEBUG_COLS = {
            "atendSttusNm", "trneeSttusNm", "lpsilTime", "levromTime",
            "gnotBeginTime", "gnotRtrnTime", "gnotInputYn", "octhtInputYn"
    };

    private final HrdBoardService boardService;
    private final HrdNetClient client;
    private final HrdRequestTemplateProvider templates;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return boardService.status();
    }

    /** 한 과정의 출결 원시 시각/상태 컬럼을 덤프(필드 매핑 확인용, 이름 제외). */
    @GetMapping("/debug")
    public Object debug(@RequestParam String tracseId, @RequestParam String tracseTme) {
        if (templates.detailTemplate().isEmpty()) {
            return Map.of("error", "템플릿 없음");
        }
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        SsvData data = client.fetchDetailRaw(templates.detailTemplate().get(), tracseId, tracseTme, today);
        SsvDataset roster = data.getDataset("ds_dailAtendList");
        if (roster == null) {
            return Map.of("error", "ds_dailAtendList 없음");
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 0; i < roster.getRowCount(); i++) {
            Map<String, String> row = new LinkedHashMap<>();
            for (String col : DEBUG_COLS) {
                if (roster.hasColumn(col)) {
                    row.put(col, roster.getString(i, col));
                }
            }
            rows.add(row);
        }
        return Map.of("rowCount", roster.getRowCount(), "rows", rows);
    }
}
