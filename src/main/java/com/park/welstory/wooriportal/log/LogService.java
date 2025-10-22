package com.park.welstory.wooriportal.log;

import com.park.welstory.wooriportal.pcinfo.PcInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LogService {

    private final LogRepository logRepository;
    private final PcInfoRepository pcInfoRepository;


    // 로그 저장
    public LogDTO saveLog(LogDTO dto) {
        LogEntity entity = new LogEntity();
        entity.setContent(dto.getLogContent());
        entity.setPcInfo(pcInfoRepository.findById(dto.getPcinfoNum()).orElseThrow(() -> new RuntimeException("PC를 찾을 수 없음")));
        LogEntity savedEntity = logRepository.save(entity);
        return toDTO(savedEntity);
    }

    // PC 관련 로그 저장
    public void savePcLog(Long pcinfoNum, String content) {
        LogEntity entity = new LogEntity();
        
        // PC 정보 조회하여 content에 모든 정보 포함
        if (pcinfoNum != null) {
            try {
                var pcInfo = pcInfoRepository.findById(pcinfoNum).orElse(null);
                if (pcInfo != null) {
                    String buildingName = "미지정";
                    String roomName = "미지정";
                    String seatNum = pcInfo.getPcInfoSeatNum() != null ? pcInfo.getPcInfoSeatNum() : "미지정";
                    
                    if (pcInfo.getLocation() != null) {
                        roomName = pcInfo.getLocation().getLocationName();
                        if (pcInfo.getLocation().getLocationParent() != null) {
                            buildingName = pcInfo.getLocation().getLocationParent().getLocationName();
                        }
                    }
                    
                    // content에 위치 정보 포함
                    String fullContent = "[" + buildingName + "] [" + roomName + "] [" + seatNum + "] " + content;
                    entity.setContent(fullContent);
                    
                } else {
                    entity.setContent(content);
                }
            } catch (Exception e) {
                entity.setContent(content);
            }
        } else {
            entity.setContent(content);
        }
        
        logRepository.save(entity);
    }

    // Entity를 DTO로 변환
    public LogDTO toDTO(LogEntity entity) {
        LogDTO dto = new LogDTO();
        dto.setLogNum(entity.getId());
        dto.setLogContent(entity.getContent());
        dto.setCreatedAt(entity.getCreatedAt());
        
        return dto;
    }

    // 최근 로그 조회
    public Page<LogDTO> getRecentLogs(Pageable pageable) {
        Page<LogEntity> logPage = logRepository.findAllByOrderByCreatedAtDesc(pageable);
        return logPage.map(this::toDTO);
    }

    // 로그 삭제
    public void deleteLog(Long logNum) {
        logRepository.deleteById(logNum);
    }
} 