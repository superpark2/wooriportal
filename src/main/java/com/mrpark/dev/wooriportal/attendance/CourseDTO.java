package com.mrpark.dev.wooriportal.attendance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class CourseDTO {

    private Long id;
    private String courseName;

    /** "1,2,3,4,5" 형식 */
    private String daysOfWeek;

    private String startDate;   // "yyyy-MM-dd" or null
    private String endDate;     // "yyyy-MM-dd" or null

    /** "HH:mm" 표시용 (Entity는 HHMM 저장) */
    private String checkInTime;
    private String checkOutTime;

    private int lateGraceMinutes;
    private boolean active;

    // ── Entity → DTO ────────────────────────────────────────
    public static CourseDTO from(CourseEntity e) {
        CourseDTO dto = new CourseDTO();
        dto.setId(e.getId());
        dto.setCourseName(e.getCourseName());
        dto.setDaysOfWeek(e.getDaysOfWeek());
        dto.setStartDate(e.getStartDate() != null ? e.getStartDate().toString() : null);
        dto.setEndDate(e.getEndDate() != null ? e.getEndDate().toString() : null);
        dto.setCheckInTime(toDisplay(e.getCheckInTime()));
        dto.setCheckOutTime(toDisplay(e.getCheckOutTime()));
        dto.setLateGraceMinutes(e.getLateGraceMinutes());
        dto.setActive(e.isActive());
        return dto;
    }

    // ── DTO → Entity (create/update) ────────────────────────
    public void applyTo(CourseEntity e) {
        e.setCourseName(courseName);
        e.setDaysOfWeek(daysOfWeek);
        e.setStartDate(startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate) : null);
        e.setEndDate(endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : null);
        e.setCheckInTime(toHhmm(checkInTime));
        e.setCheckOutTime(toHhmm(checkOutTime));
        e.setLateGraceMinutes(lateGraceMinutes);
        e.setActive(active);
    }

    /** "0900" → "09:00" */
    private static String toDisplay(String hhmm) {
        if (hhmm == null || hhmm.length() != 4) return hhmm;
        return hhmm.substring(0, 2) + ":" + hhmm.substring(2);
    }

    /** "09:00" or "0900" → "0900" */
    private static String toHhmm(String time) {
        if (time == null || time.isBlank()) return null;
        return time.replace(":", "");
    }
}
