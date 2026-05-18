package com.mrpark.dev.wooriportal.global.util;

import com.mrpark.dev.wooriportal.location.LocationEntity;
import com.mrpark.dev.wooriportal.log.LogService;
import com.mrpark.dev.wooriportal.pcinfo.PcInfoEntity;
import jakarta.persistence.PreRemove;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA EntityListener — PcInfoEntity 삭제 시 로그 자동 기록.
 *
 * 주의: @PrePersist, @PreUpdate는 PcInfoService에서 직접 처리하므로
 * 여기서는 @PreRemove만 담당한다.
 */
@Component
public class LogUtil {

    private static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        LogUtil.applicationContext = applicationContext;
    }

    // 버그수정: 빈 @PrePersist, @PreUpdate 메서드 제거 (PcInfoService에서 직접 처리)

    @PreRemove
    public void onPcRemove(Object entity) {
        if (!(entity instanceof PcInfoEntity pc)) return;

        String locationName = pc.getLocation() != null
                ? buildLocationString(pc.getLocation()) : "미지정 위치";
        String seatNum = pc.getPcInfoSeatNum() != null ? pc.getPcInfoSeatNum() : "미지정";
        saveLogStatic(pc.getPcInfoNum(),
                "PC 삭제됨 (이전 위치: " + locationName + ", 좌석: " + seatNum + ")");
    }

    public static void logPcRegistration(PcInfoEntity pc) {
        if (pc == null) return;
        String locationName = pc.getLocation() != null
                ? buildLocationString(pc.getLocation()) : "미지정 위치";
        String seatNum = pc.getPcInfoSeatNum() != null ? pc.getPcInfoSeatNum() : "미지정";
        saveLogStatic(pc.getPcInfoNum(),
                "신규 PC 등록됨 (위치: " + locationName + ", 좌석: " + seatNum + ")");
    }

    public static void logPcLocationChange(PcInfoEntity oldPc, PcInfoEntity newPc) {
        if (oldPc == null || newPc == null) return;
        Long oldLocNum = oldPc.getLocation() != null ? oldPc.getLocation().getLocationNum() : null;
        Long newLocNum = newPc.getLocation() != null ? newPc.getLocation().getLocationNum() : null;
        if (Objects.equals(oldLocNum, newLocNum)) return;

        String oldLocation = oldPc.getLocation() != null ? buildLocationString(oldPc.getLocation()) : "미지정 위치";
        String newLocation = newPc.getLocation() != null ? buildLocationString(newPc.getLocation()) : "미지정 위치";
        String seatNum = newPc.getPcInfoSeatNum() != null ? newPc.getPcInfoSeatNum() : "미지정";
        saveLogStatic(newPc.getPcInfoNum(),
                "위치 이동: " + oldLocation + " → " + newLocation + " (좌석: " + seatNum + ")");
    }

    public static void logPcSpecChange(PcInfoEntity oldPc, PcInfoEntity newPc) {
        if (oldPc == null || newPc == null) return;
        List<String> changes = new ArrayList<>();

        appendIfChanged(changes, "CPU",      oldPc.getPcInfoCpu(),     newPc.getPcInfoCpu());
        appendIfChanged(changes, "RAM",      oldPc.getPcInfoRam(),     newPc.getPcInfoRam());
        appendIfChanged(changes, "저장장치", oldPc.getPcInfoStorage(), newPc.getPcInfoStorage());
        appendIfChanged(changes, "VGA",      oldPc.getPcInfoVga(),     newPc.getPcInfoVga());
        appendIfChanged(changes, "모니터",   oldPc.getPcInfoMonitor(), newPc.getPcInfoMonitor());
        appendIfChanged(changes, "IP",       oldPc.getPcInfoIp(),      newPc.getPcInfoIp());
        appendIfChanged(changes, "좌석번호", oldPc.getPcInfoSeatNum(), newPc.getPcInfoSeatNum());

        if (!changes.isEmpty()) {
            saveLogStatic(newPc.getPcInfoNum(), "정보 변경: " + String.join(", ", changes));
        }
    }

    private static void appendIfChanged(List<String> changes, String field, String oldVal, String newVal) {
        if (!Objects.equals(nvl(oldVal), nvl(newVal))) {
            changes.add(field + ": " + nvl(oldVal) + " → " + nvl(newVal));
        }
    }

    private static String nvl(String str) {
        return (str == null || str.isEmpty()) ? "없음" : str;
    }

    private static String buildLocationString(LocationEntity location) {
        if (location == null) return "미지정";
        StringBuilder sb = new StringBuilder();
        if (location.getLocationParent() != null) {
            sb.append(location.getLocationParent().getLocationName()).append(" ");
        }
        sb.append(location.getLocationName());
        return sb.toString();
    }

    public static void saveLogStatic(Long pcinfoNum, String content) {
        if (applicationContext == null) return;
        try {
            applicationContext.getBean(LogService.class).savePcLog(pcinfoNum, content);
        } catch (Exception ignored) {
        }
    }
}
