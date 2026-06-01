package com.mrpark.dev.wooriportal.attendance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ManualAttendanceRequest {
    private String courseName;
    private String studentName;
    /** 회차(기수) — 과정 구분용 */
    private Integer round;
    /** "ENTRY" 또는 "EXIT" */
    private String type;
    /** 출결 방식: "QR" / "BEACON" / "MANUAL" */
    private String method;
    /** "HHmm" 또는 "HH:mm" 형식 */
    private String time;
}
