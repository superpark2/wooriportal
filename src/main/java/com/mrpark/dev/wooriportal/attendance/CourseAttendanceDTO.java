package com.mrpark.dev.wooriportal.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CourseAttendanceDTO {

    private Long courseId;
    private String courseName;
    private Integer round;          // 회차(기수)
    private String daysOfWeek;      // "1,2,3,4,5" — 화면에서 요일 표시용
    private String checkInTime;
    private String checkOutTime;
    private List<StudentAttendanceItem> students;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class StudentAttendanceItem {
        private Long studentId;
        private String studentName;
        private String checkIn;         // "HH:mm" or null
        private String checkOut;        // "HH:mm" or null
        private String status;          // AttendanceStatus.name()
        private String statusLabel;     // 한글 라벨
        private String colorClass;      // CSS 클래스명 (absent / present / exited / late)
    }
}
