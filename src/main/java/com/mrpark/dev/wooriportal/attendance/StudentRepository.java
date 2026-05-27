package com.mrpark.dev.wooriportal.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    List<StudentEntity> findByCourseIdAndActiveTrueOrderByStudentNameAsc(Long courseId);

    List<StudentEntity> findByCourseIdOrderByStudentNameAsc(Long courseId);
}
