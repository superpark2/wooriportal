package com.mrpark.dev.wooriportal.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<CourseEntity, Long> {

    List<CourseEntity> findAllByOrderByCourseNameAsc();

    List<CourseEntity> findByActiveTrueOrderByCourseNameAsc();
}
