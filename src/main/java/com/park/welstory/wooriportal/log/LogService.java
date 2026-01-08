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


    // 로그 저장 (pcinfo 정보 포함)
    public LogDTO saveLog(LogDTO dto) {
        LogEntity entity = new LogEntity();
        entity.setContent(dto.getLogContent());
        
        // PC 정보 조회 및 설정
        if (dto.getPcinfoNum() != null) {
            var pcInfo = pcInfoRepository.findById(dto.getPcinfoNum())
                .orElseThrow(() -> new RuntimeException("PC를 찾을 수 없음"));
            entity.setPcInfo(pcInfo);
        }
        
        LogEntity savedEntity = logRepository.save(entity);
        return toDTO(savedEntity);
    }

    // PC 관련 로그 저장 (pcinfo 정보 포함)
    public void savePcLog(Long pcinfoNum, String content) {
        LogEntity entity = new LogEntity();
        
        // PC 정보 조회 및 설정
        if (pcinfoNum != null) {
            try {
                var pcInfo = pcInfoRepository.findById(pcinfoNum).orElse(null);
                if (pcInfo != null) {
                    entity.setPcInfo(pcInfo);
                    
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

    // Entity를 DTO로 변환 (pcinfo 정보 포함)
    public LogDTO toDTO(LogEntity entity) {
        LogDTO dto = new LogDTO();
        dto.setLogNum(entity.getId());
        dto.setLogContent(entity.getContent());
        dto.setCreatedAt(entity.getCreatedAt());
        
        // PC 정보 설정
        if (entity.getPcInfo() != null) {
            dto.setPcinfoNum(entity.getPcInfo().getPcInfoNum());
            
            String buildingName = "미지정";
            String roomName = "미지정";
            String seatNum = entity.getPcInfo().getPcInfoSeatNum() != null ? entity.getPcInfo().getPcInfoSeatNum() : "미지정";
            
            if (entity.getPcInfo().getLocation() != null) {
                roomName = entity.getPcInfo().getLocation().getLocationName();
                if (entity.getPcInfo().getLocation().getLocationParent() != null) {
                    buildingName = entity.getPcInfo().getLocation().getLocationParent().getLocationName();
                }
            }
            
            dto.setBuildingName(buildingName);
            dto.setRoomName(roomName);
            dto.setSeatNum(seatNum);
        }
        
        return dto;
    }

    // 최근 로그 조회
    public Page<LogDTO> getRecentLogs(Pageable pageable) {
        Page<LogEntity> logPage = logRepository.findAllByOrderByCreatedAtDesc(pageable);
        return logPage.map(this::toDTO);
    }
    
    // 특정 PC의 로그 조회
    public Page<LogDTO> getLogsByPcInfoNum(Long pcInfoNum, Pageable pageable) {
        Page<LogEntity> logPage = logRepository.findByPcInfoNumOrderByCreatedAtDesc(pcInfoNum, pageable);
        return logPage.map(this::toDTO);
    }

    // 로그 삭제
    public void deleteLog(Long logNum) {
        logRepository.deleteById(logNum);
    }
    
    // 통합 로그 처리 (저장/수정/삭제)
    public LogDTO processLog(Long pcinfoNum, String content, String action) {
        if (pcinfoNum == null) {
            throw new RuntimeException("PC 정보가 필요합니다.");
        }
        
        // PC 정보 조회
        var pcInfo = pcInfoRepository.findById(pcinfoNum)
            .orElseThrow(() -> new RuntimeException("PC를 찾을 수 없음"));
        
        // 로그 엔티티 생성
        LogEntity entity = new LogEntity();
        entity.setPcInfo(pcInfo);
        
        // 위치 정보 추출
        String buildingName = "미지정";
        String roomName = "미지정";
        String seatNum = pcInfo.getPcInfoSeatNum() != null ? pcInfo.getPcInfoSeatNum() : "미지정";
        
        if (pcInfo.getLocation() != null) {
            roomName = pcInfo.getLocation().getLocationName();
            if (pcInfo.getLocation().getLocationParent() != null) {
                buildingName = pcInfo.getLocation().getLocationParent().getLocationName();
            }
        }
        
        // 액션에 따른 content 설정
//        String fullContent = "[" + buildingName + "] [" + roomName + "] [" + seatNum + "] " + action + ": " + content;
//        entity.setContent(fullContent);

        entity.setContent(content);

        // 저장
        LogEntity savedEntity = logRepository.save(entity);
        return toDTO(savedEntity);
    }
} 