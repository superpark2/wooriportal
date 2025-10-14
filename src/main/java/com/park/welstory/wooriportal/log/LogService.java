package com.park.welstory.wooriportal.log;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LogService {

    private final LogRepository qrLogRepository;

    // admin.password 미사용

    // PC별 QR 로그 조회
    public Page<LogDTO> getQrLogsByPcinfoNum(Long pcinfoNum, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LogEntity> logPage = qrLogRepository.findByPcinfoNum(pcinfoNum, pageable);
        return logPage.map(this::toDTO);
    }

    // QR 로그 저장 (PC 전용)
    public void savePcQrLog(Long pcinfoNum, String content) {
        LogEntity entity = new LogEntity();
        entity.setContent(content);
        entity.setPcinfoNum(pcinfoNum);
        qrLogRepository.save(entity);
    }

    // Entity를 DTO로 변환
    public LogDTO toDTO(LogEntity entity) {
        LogDTO dto = new LogDTO();
        dto.setLogNum(entity.getId());
        dto.setLogContent(entity.getContent());
        dto.setPcinfoNum(entity.getPcinfoNum());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    // 최근 QR 로그 조회
    public Page<LogDTO> getRecentQrLogs(Pageable pageable) {
        Page<LogEntity> logPage = qrLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return logPage.map(this::toDTO);
    }

    // QR 로그 삭제
    public void deleteQrLog(Long qrlogNum) {
        qrLogRepository.deleteById(qrlogNum);
    }

    // 비밀번호 검증 로직 제거됨
} 