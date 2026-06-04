package com.mrpark.dev.wooriportal.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface CourseDayModeRepository extends JpaRepository<CourseDayModeEntity, Long> {

    Optional<CourseDayModeEntity> findByStudentIdAndDate(Long studentId, LocalDate date);

    /** 학생 삭제 시 */
    void deleteByStudentId(Long studentId);

    /** 과정 삭제 시 — 해당 과정 학생 ID 목록 일괄 삭제 */
    void deleteByStudentIdIn(Collection<Long> studentIds);
}
