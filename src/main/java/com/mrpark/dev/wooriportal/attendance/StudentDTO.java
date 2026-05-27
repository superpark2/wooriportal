package com.mrpark.dev.wooriportal.attendance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class StudentDTO {

    private Long id;
    private Long courseId;
    private String studentName;
    /** 생년월일 앞자리 (선택 — 동명 2인 구분용) */
    private String birthPrefix;
    private boolean active;

    public static StudentDTO from(StudentEntity e) {
        StudentDTO dto = new StudentDTO();
        dto.setId(e.getId());
        dto.setCourseId(e.getCourse().getId());
        dto.setStudentName(e.getStudentName());
        dto.setBirthPrefix(e.getBirthPrefix());
        dto.setActive(e.isActive());
        return dto;
    }
}
