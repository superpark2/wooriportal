package com.park.welstory.wooriportal.common.board;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class BoardTitleService {

    private static final Map<String, String> GROUP_TITLE_MAP = Map.ofEntries(
            Map.entry("sales", "영업팀"),
            Map.entry("management", "행정"),
            Map.entry("facility", "시설장비"),
            Map.entry("personal", "개인"),
            Map.entry("common", "통합")
    );

    private static final Map<String, String> CATEGORY_SUBTITLE_MAP = Map.ofEntries(
            Map.entry("notice", "공지사항"),
            Map.entry("knowledge", "업무지식"),
            Map.entry("library", "자료실"),
            Map.entry("board", "게시판"),
            Map.entry("worklog", "업무일지"),
            Map.entry("favorites", "즐겨찾기"),
            Map.entry("recent", "최근 열람 자료")
    );

    public String boardTitle(String group) {
        return GROUP_TITLE_MAP.getOrDefault(group, "");
    }

    public String boardSubTitle(String category) {
        return CATEGORY_SUBTITLE_MAP.getOrDefault(category, "");
    }


}