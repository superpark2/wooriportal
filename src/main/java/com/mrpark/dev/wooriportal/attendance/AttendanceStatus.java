package com.mrpark.dev.wooriportal.attendance;

public enum AttendanceStatus {
    ABSENT,                // 미출석 → 회색
    PRESENT,               // 재원중 (정상 입실, 미퇴실) → 초록
    EXITED,                // 정상 퇴실 → 파랑
    LATE,                  // 지각 (퇴실 미확인) → 주황
    LATE_EXITED,           // 지각 후 퇴실 → 주황
    EARLY_LEAVE,           // 조퇴 (정상 입실) → 주황
    LATE_EARLY_LEAVE       // 지각+조퇴 → 주황
}
