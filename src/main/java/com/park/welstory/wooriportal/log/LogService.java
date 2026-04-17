package com.park.welstory.wooriportal.log;

import com.park.welstory.wooriportal.global.sse.SseService;
import com.park.welstory.wooriportal.pcinfo.PcInfoEntity;
import com.park.welstory.wooriportal.pcinfo.PcInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final PcInfoRepository pcInfoRepository;
    private final SseService sseService;

    /**
     * 핵심 로그 저장 메서드 - 모든 경로가 여기로 모임
     * 태그를 포함하여 전체 로그 시스템(PCLOG)에 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePcLog(Long pcinfoNum, String content) {
        if (content == null || content.isBlank()) return;

        LogEntity entity = new LogEntity();
        String finalContent = content;

        if (pcinfoNum != null) {
            PcInfoEntity pcInfo = pcInfoRepository.findById(pcinfoNum).orElse(null);
            if (pcInfo != null) {
                entity.setPcInfo(pcInfo);

                // 태그 생성 복구: [건물] [실] [좌석]
                String buildingName = "미지정";
                String roomName     = "미지정";
                String seatNum      = pcInfo.getPcInfoSeatNum() != null ? pcInfo.getPcInfoSeatNum() : "미지정";

                if (pcInfo.getLocation() != null) {
                    roomName = pcInfo.getLocation().getLocationName();
                    if (pcInfo.getLocation().getLocationParent() != null) {
                        buildingName = pcInfo.getLocation().getLocationParent().getLocationName();
                    }
                }

                finalContent = "[" + buildingName + "] [" + roomName + "] [" + seatNum + "] " + content;
            }
        }

        entity.setContent(finalContent);
        logRepository.save(entity);
        
        // SSE 실시간 알림 전송 (전체 태그 포함된 내용으로 전송)
        sseService.sendLogNotification(finalContent);
    }

    @Transactional(readOnly = true)
    public Page<LogDTO> getRecentLogs(Pageable pageable) {
        return logRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LogDTO> getLogsByPcInfoNum(Long pcInfoNum, Pageable pageable) {
        return logRepository.findByPcInfoNumOrderByCreatedAtDesc(pcInfoNum, pageable).map(this::toDTO);
    }

    @Transactional
    public void deleteLog(Long logNum) {
        logRepository.deleteById(logNum);
    }

    public LogDTO toDTO(LogEntity entity) {
        LogDTO dto = new LogDTO();
        dto.setLogNum(entity.getId());
        
        String content = entity.getContent();
        dto.setLogContent(content);
        dto.setCreatedAt(entity.getCreatedAt());

        if (entity.getPcInfo() != null) {
            PcInfoEntity pcInfo = entity.getPcInfo();
            dto.setPcinfoNum(pcInfo.getPcInfoNum());
            dto.setSeatNum(pcInfo.getPcInfoSeatNum() != null ? pcInfo.getPcInfoSeatNum() : "미지정");

            if (pcInfo.getLocation() != null) {
                dto.setRoomName(pcInfo.getLocation().getLocationName());
                if (pcInfo.getLocation().getLocationParent() != null) {
                    dto.setBuildingName(pcInfo.getLocation().getLocationParent().getLocationName());
                }
            }
            
            // pcinfo 전용 뷰를 위해 태그를 제거한 순수 내용만 추출 (정규식 사용)
            // "[건물] [실] [좌석] " 형태를 제거합니다.
            if (content != null && content.startsWith("[")) {
                String cleanContent = content.replaceFirst("^\\[.*?\\] \\s*\\[.*?\\] \\s*\\[.*?\\]\\s*", "");
                dto.setCleanContent(cleanContent); // DTO에 새로운 필드가 있다고 가정하거나, 
                // 만약 필드가 없다면 프론트엔드에서 처리하도록 logContent에 그대로 둡니다.
            } else {
                dto.setCleanContent(content);
            }
        }
        return dto;
    }
}