package com.park.welstory.wooriportal.global.util;

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

    @PrePersist
    public void onPcPersist(Object entity) {
    }

    @PreRemove
    public void onPcRemove(Object entity) {
        if (entity instanceof PcInfoEntity) {
            PcInfoEntity pc = (PcInfoEntity) entity;
            String locationName = pc.getLocation() != null ? buildLocationStringStatic(pc.getLocation()) : "미지정 위치";
            String seatNum = pc.getPcInfoSeatNum() != null ? pc.getPcInfoSeatNum() : "미지정";

            String content = "PC 삭제됨 (이전 위치: " + locationName + ", 좌석: " + seatNum + ")";
            saveLogStatic(pc.getPcInfoNum(), content);
        }
    }

    @PreUpdate
    public void onPcUpdate(Object entity) {
    }

    public static void logPcRegistration(PcInfoEntity pc) {
        if (pc == null) return;
        String locationName = pc.getLocation() != null ? buildLocationStringStatic(pc.getLocation()) : "미지정 위치";
        String seatNum = pc.getPcInfoSeatNum() != null ? pc.getPcInfoSeatNum() : "미지정";
        String content = "신규 PC 등록됨 (위치: " + locationName + ", 좌석: " + seatNum + ")";
        saveLogStatic(pc.getPcInfoNum(), content);
    }

    public static void logPcLocationChange(PcInfoEntity oldPc, PcInfoEntity newPc) {
        if (oldPc == null || newPc == null) return;

        Long oldLocNum = (oldPc.getLocation() != null) ? oldPc.getLocation().getLocationNum() : null;
        Long newLocNum = (newPc.getLocation() != null) ? newPc.getLocation().getLocationNum() : null;

        if (!Objects.equals(oldLocNum, newLocNum)) {
            String oldLocation = (oldPc.getLocation() != null) ? buildLocationStringStatic(oldPc.getLocation()) : "미지정 위치";
            String newLocation = (newPc.getLocation() != null) ? buildLocationStringStatic(newPc.getLocation()) : "미지정 위치";
            String seatNum = newPc.getPcInfoSeatNum() != null ? newPc.getPcInfoSeatNum() : "미지정";

            String content = "위치 이동: " + oldLocation + " → " + newLocation + " (좌석: " + seatNum + ")";
            saveLogStatic(newPc.getPcInfoNum(), content);
        }
    }

    public static void logPcSpecChange(PcInfoEntity oldPc, PcInfoEntity newPc) {
        if (oldPc == null || newPc == null) return;
        List<String> changes = new ArrayList<>();

        if (!Objects.equals(oldPc.getPcInfoCpu(), newPc.getPcInfoCpu()))
            changes.add("CPU: " + nvl(oldPc.getPcInfoCpu()) + " → " + nvl(newPc.getPcInfoCpu()));
        if (!Objects.equals(oldPc.getPcInfoRam(), newPc.getPcInfoRam()))
            changes.add("RAM: " + nvl(oldPc.getPcInfoRam()) + " → " + nvl(newPc.getPcInfoRam()));
        if (!Objects.equals(oldPc.getPcInfoStorage(), newPc.getPcInfoStorage()))
            changes.add("저장장치: " + nvl(oldPc.getPcInfoStorage()) + " → " + nvl(newPc.getPcInfoStorage()));
        if (!Objects.equals(oldPc.getPcInfoVga(), newPc.getPcInfoVga()))
            changes.add("VGA: " + nvl(oldPc.getPcInfoVga()) + " → " + nvl(newPc.getPcInfoVga()));
        if (!Objects.equals(oldPc.getPcInfoMonitor(), newPc.getPcInfoMonitor()))
            changes.add("모니터: " + nvl(oldPc.getPcInfoMonitor()) + " → " + nvl(newPc.getPcInfoMonitor()));
        if (!Objects.equals(oldPc.getPcInfoIp(), newPc.getPcInfoIp()))
            changes.add("IP: " + nvl(oldPc.getPcInfoIp()) + " → " + nvl(newPc.getPcInfoIp()));
        if (!Objects.equals(oldPc.getPcInfoSeatNum(), newPc.getPcInfoSeatNum()))
            changes.add("좌석번호: " + nvl(oldPc.getPcInfoSeatNum()) + " → " + nvl(newPc.getPcInfoSeatNum()));

        if (!changes.isEmpty()) {
            saveLogStatic(newPc.getPcInfoNum(), "정보 변경: " + String.join(", ", changes));
        }
    }

    private static String nvl(String str) {
        return (str == null || str.isEmpty()) ? "없음" : str;
    }

    private static String buildLocationStringStatic(LocationEntity location) {
        if (location == null) return "미지정";
        StringBuilder sb = new StringBuilder();
        if (location.getLocationParent() != null) {
            sb.append(location.getLocationParent().getLocationName()).append(" ");
        }
        sb.append(location.getLocationName());
        return sb.toString();
    }

    public static void saveLogStatic(Long pcinfoNum, String content) {
        try {
            if (applicationContext != null) {
                LogService logService = applicationContext.getBean(LogService.class);
                logService.savePcLog(pcinfoNum, content);
            }
        } catch (Exception e) {
        }
    }
}
