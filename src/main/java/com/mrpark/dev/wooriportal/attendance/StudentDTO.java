package com.mrpark.dev.wooriportal.attendance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class StudentDTO {

    private Long id;
    private Long courseId;
    private String studentName;
    private String cardNum;
    private boolean active;

    public static StudentDTO from(StudentEntity e) {
        StudentDTO dto = new StudentDTO();
        dto.setId(e.getId());
        dto.setCourseId(e.getCourse().getId());
        dto.setStudentName(e.getStudentName());
        dto.setCardNum(e.getCardNum());
        dto.setActive(e.isActive());
        return dto;
    }
}
