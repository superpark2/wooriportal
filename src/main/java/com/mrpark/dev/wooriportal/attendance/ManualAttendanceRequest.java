package com.mrpark.dev.wooriportal.attendance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ManualAttendanceRequest {
    private String courseName;
    private String studentName;
    /** "ENTRY" 또는 "EXIT" */
    private String type;
    /** "HHmm" 또는 "HH:mm" 형식 */
    private String time;
}
