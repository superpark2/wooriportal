package com.mrpark.dev.wooriportal.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    List<StudentEntity> findByCourseIdAndActiveTrueOrderByStudentNameAsc(Long courseId);

    List<StudentEntity> findByCourseIdOrderByStudentNameAsc(Long courseId);

    /** 자동 등록 시 중복 방지용 — 과정 내 동일 이름 학생 조회 (활성 여부 무관) */
    Optional<StudentEntity> findFirstByCourseIdAndStudentName(Long courseId, String studentName);
}
