package com.mrpark.dev.wooriportal.attendance;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
public class AttendanceLogDTO {

    private Long id;
    private String cardNum;
    private String studentName;
    private String courseName;

    /** "1202" → 표시용 "12:02" */
    private String checkIn;
    private String checkOut;   // null 이면 아직 퇴실 안함

    /** "재원중" | "퇴실" */
    private String status;

    private LocalDate attendanceDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    static AttendanceLogDTO from(AttendanceLogEntity e) {
        AttendanceLogDTO dto = new AttendanceLogDTO();
        dto.setId(e.getId());
        dto.setCardNum(e.getCardNum());
        dto.setStudentName(e.getStudentName());
        dto.setCourseName(e.getCourseName());
        dto.setCheckIn(formatTime(e.getCheckIn()));
        dto.setCheckOut(formatTime(e.getCheckOut()));
        dto.setStatus(e.getCheckOut() == null ? "재원중" : "퇴실");
        dto.setAttendanceDate(e.getAttendanceDate());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    /** "1202" → "12:02", null → null */
    private static String formatTime(String hhmm) {
        if (hhmm == null || hhmm.length() != 4) return hhmm;
        return hhmm.substring(0, 2) + ":" + hhmm.substring(2);
    }
}
