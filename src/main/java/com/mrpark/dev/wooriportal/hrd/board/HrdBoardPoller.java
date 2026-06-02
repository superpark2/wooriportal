package com.mrpark.dev.wooriportal.hrd.board;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 전광판 출결 폴러. 기본 10초 주기로 등록 과정의 당일 출결을 HRD 에서 끌어온다.
 *
 * <p>{@code fixedDelay} 라 직전 갱신이 끝난 뒤 간격을 둬, 호출이 겹치지 않는다.</p>
 */
@Component
@RequiredArgsConstructor
public class HrdBoardPoller {

    private final HrdBoardService boardService;

    @Scheduled(fixedDelayString = "${hrd.board.poll-ms:10000}", initialDelayString = "${hrd.board.initial-delay-ms:5000}")
    public void poll() {
        boardService.refresh();
    }
}
