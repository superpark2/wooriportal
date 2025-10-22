package com.park.welstory.wooriportal.util;

import com.park.welstory.wooriportal.location.LocationEntity;
import com.park.welstory.wooriportal.log.LogService;
import com.park.welstory.wooriportal.pcinfo.PcInfoEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class LogUtil {
    
    private static ApplicationContext applicationContext;
    
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        LogUtil.applicationContext = applicationContext;
    }
    
    // PC 등록 이벤트 처리
    @PrePersist
    public void onPcPersist(Object entity) {
        if (entity instanceof PcInfoEntity) {
            PcInfoEntity pc = (PcInfoEntity) entity;
            if (pc.getLocation() != null) {
                String content = buildLocationString(pc.getLocation()) + "에 " + 
                               (pc.getPcInfoSeatNum() != null ? pc.getPcInfoSeatNum() : "") + "번 PC가 등록됨.";
                saveLog(pc.getPcInfoNum(), content);
            }
        }
    }
    
    // PC 삭제 이벤트 처리
    @PreRemove
    public void onPcRemove(Object entity) {
        if (entity instanceof PcInfoEntity) {
            PcInfoEntity pc = (PcInfoEntity) entity;
            
            // PC 정보를 미리 저장해둠 (삭제 전에)
            Long pcInfoNum = pc.getPcInfoNum();
            String seatNum = pc.getPcInfoSeatNum();
            String locationName = "미지정";
            
            try {
                if (pc.getLocation() != null) {
                    locationName = buildLocationString(pc.getLocation());
                }
            } catch (Exception e) {
                // 위치 정보 조회 실패 시 기본값 사용
            }
            
            String content = locationName + "에 " + (seatNum != null ? seatNum : "") + "번 PC가 삭제됨.";
            saveLog(pcInfoNum, content);
        }
    }
    
    // PC 수정 이벤트 처리
    @PreUpdate
    public void onPcUpdate(Object entity) {
        // 수정 로그는 서비스에서 처리 (이전 값과 비교하기 위해)
    }
    
    // 위치 문자열 생성
    private String buildLocationString(LocationEntity location) {
        StringBuilder sb = new StringBuilder();
        
        if (location.getLocationParent() != null) {
            // 룸인 경우: 빌딩명 (룸명)
            sb.append(location.getLocationParent().getLocationName())
              .append(" (")
              .append(location.getLocationName())
              .append(")");
        } else {
            // 빌딩인 경우: 빌딩명만
            sb.append(location.getLocationName());
        }
        
        return sb.toString();
    }
    
    // 위치 변경 감지를 위한 수동 로그 메서드
    public static void logPcLocationChange(PcInfoEntity oldPc, PcInfoEntity newPc) {
        if (oldPc != null && newPc != null && 
            oldPc.getLocation() != null && newPc.getLocation() != null &&
            !oldPc.getLocation().getLocationNum().equals(newPc.getLocation().getLocationNum())) {
            
            String oldLocation = buildLocationStringStatic(oldPc.getLocation());
            String newLocation = buildLocationStringStatic(newPc.getLocation());
            String seatNum = oldPc.getPcInfoSeatNum() != null ? oldPc.getPcInfoSeatNum() : "없음";
            String content = oldLocation + "에 " + seatNum + "번 PC가 " + newLocation + "로 이동됨.";
            saveLogStatic(newPc.getPcInfoNum(), content);
        }
    }
    
    private static String buildLocationStringStatic(LocationEntity location) {
        StringBuilder sb = new StringBuilder();
        
        if (location.getLocationParent() != null) {
            sb.append(location.getLocationParent().getLocationName())
              .append(" (")
              .append(location.getLocationName())
              .append(")");
        } else {
            sb.append(location.getLocationName());
        }
        
        return sb.toString();
    }
    
    // 로그 저장 (인스턴스 메서드)
    private void saveLog(Long pcinfoNum, String content) {
        try {
            if (applicationContext != null) {
                LogService logService = applicationContext.getBean(LogService.class);
                logService.savePcLog(pcinfoNum, content);
            }
        } catch (Exception e) {
            // 로그 저장 실패 시 무시
        }
    }
    
    // 로그 저장 (정적 메서드)
    public static void saveLogStatic(Long pcinfoNum, String content) {
        try {
            if (applicationContext != null) {
                LogService logService = applicationContext.getBean(LogService.class);
                logService.savePcLog(pcinfoNum, content);
            }
        } catch (Exception e) {
            // 로그 저장 실패 시 무시
        }
    }
    
    // PC 스펙 변경 로그
    public static void logPcSpecChange(PcInfoEntity oldPc, PcInfoEntity newPc) {
        if (oldPc == null || newPc == null) return;
        Long pcinfoNum = newPc.getPcInfoNum();
        
        // 변경된 항목들을 수집
        List<String> changes = new ArrayList<>();
        
        // CPU 변경
        if (!Objects.equals(oldPc.getPcInfoCpu(), newPc.getPcInfoCpu())) {
            String oldCpu = oldPc.getPcInfoCpu() != null ? oldPc.getPcInfoCpu() : "없음";
            String newCpu = newPc.getPcInfoCpu() != null ? newPc.getPcInfoCpu() : "없음";
            changes.add("CPU: " + oldCpu + " → " + newCpu);
        }
        
        // RAM 변경
        if (!Objects.equals(oldPc.getPcInfoRam(), newPc.getPcInfoRam())) {
            String oldRam = oldPc.getPcInfoRam() != null ? oldPc.getPcInfoRam() : "없음";
            String newRam = newPc.getPcInfoRam() != null ? newPc.getPcInfoRam() : "없음";
            changes.add("RAM: " + oldRam + " → " + newRam);
        }
        
        // 저장장치 변경
        if (!Objects.equals(oldPc.getPcInfoStorage(), newPc.getPcInfoStorage())) {
            String oldStorage = oldPc.getPcInfoStorage() != null ? oldPc.getPcInfoStorage() : "없음";
            String newStorage = newPc.getPcInfoStorage() != null ? newPc.getPcInfoStorage() : "없음";
            changes.add("저장장치: " + oldStorage + " → " + newStorage);
        }
        
        // VGA 변경
        if (!Objects.equals(oldPc.getPcInfoVga(), newPc.getPcInfoVga())) {
            String oldVga = oldPc.getPcInfoVga() != null ? oldPc.getPcInfoVga() : "없음";
            String newVga = newPc.getPcInfoVga() != null ? newPc.getPcInfoVga() : "없음";
            changes.add("VGA: " + oldVga + " → " + newVga);
        }
        
        // 모니터 변경
        if (!Objects.equals(oldPc.getPcInfoMonitor(), newPc.getPcInfoMonitor())) {
            String oldMonitor = oldPc.getPcInfoMonitor() != null ? oldPc.getPcInfoMonitor() : "없음";
            String newMonitor = newPc.getPcInfoMonitor() != null ? newPc.getPcInfoMonitor() : "없음";
            changes.add("모니터: " + oldMonitor + " → " + newMonitor);
        }
        
        // IP 변경
        if (!Objects.equals(oldPc.getPcInfoIp(), newPc.getPcInfoIp())) {
            String oldIp = oldPc.getPcInfoIp() != null ? oldPc.getPcInfoIp() : "없음";
            String newIp = newPc.getPcInfoIp() != null ? newPc.getPcInfoIp() : "없음";
            changes.add("IP: " + oldIp + " → " + newIp);
        }
        
        // 좌석번호 변경
        if (!Objects.equals(oldPc.getPcInfoSeatNum(), newPc.getPcInfoSeatNum())) {
            String oldSeat = oldPc.getPcInfoSeatNum() != null ? oldPc.getPcInfoSeatNum() : "없음";
            String newSeat = newPc.getPcInfoSeatNum() != null ? newPc.getPcInfoSeatNum() : "없음";
            changes.add("좌석번호: " + oldSeat + " → " + newSeat);
        }
        
        // 변경사항이 있으면 하나의 로그로 저장
        if (!changes.isEmpty()) {
            String content = String.join(", ", changes);
            saveLogStatic(pcinfoNum, content);
        }
    }
}
