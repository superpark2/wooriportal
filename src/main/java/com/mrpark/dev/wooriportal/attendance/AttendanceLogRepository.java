package com.mrpark.dev.wooriportal.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLogEntity, Long> {

    /** 날짜별 전체 목록 (입실순) */
    List<AttendanceLogEntity> findByAttendanceDateOrderByCreatedAtAsc(LocalDate date);

    /** 과정명 + 회차 + 날짜로 전체 로그 조회 (출결 현황 빌드용) — round null/0 동일 취급 */
    @Query("SELECT a FROM AttendanceLogEntity a " +
           "WHERE a.courseName = :courseName AND a.attendanceDate = :date " +
           "AND COALESCE(a.round, 0) = COALESCE(:round, 0)")
    List<AttendanceLogEntity> findByCourseAndDate(@Param("courseName") String courseName,
                                                  @Param("round") Integer round,
                                                  @Param("date") LocalDate attendanceDate);

    /** 학생별 당일 로그 전체 (이름+회차 기준, 시간순) */
    @Query("SELECT a FROM AttendanceLogEntity a " +
           "WHERE a.courseName = :courseName AND a.attendanceDate = :date " +
           "AND a.studentName = :name AND COALESCE(a.round, 0) = COALESCE(:round, 0) " +
           "ORDER BY a.createdAt ASC")
    List<AttendanceLogEntity> findStudentLog(@Param("courseName") String courseName,
                                             @Param("round") Integer round,
                                             @Param("date") LocalDate attendanceDate,
                                             @Param("name") String studentName);

    // ── 중복 체크 (파일 재전송 시 skip 용도) ──────────────────────
    // checkIn/checkOut 에 'In'/'Out' 예약어가 포함돼 파서 오인식 → @Query 직접 작성

    /** 입실 이벤트 중복 여부 (동일 입실 시간, checkOut = null) */
    @Query("SELECT COUNT(a) > 0 FROM AttendanceLogEntity a " +
           "WHERE a.studentName = :name AND a.courseName = :courseName " +
           "AND COALESCE(a.round, 0) = COALESCE(:round, 0) " +
           "AND a.attendanceDate = :date AND a.checkIn = :checkIn AND a.checkOut IS NULL")
    boolean existsDuplicateEntry(@Param("name") String studentName,
                                 @Param("courseName") String courseName,
                                 @Param("round") Integer round,
                                 @Param("date") LocalDate attendanceDate,
                                 @Param("checkIn") String checkIn);

    /** 퇴실 이벤트 중복 여부 (동일 퇴실 시간) */
    @Query("SELECT COUNT(a) > 0 FROM AttendanceLogEntity a " +
           "WHERE a.studentName = :name AND a.courseName = :courseName " +
           "AND COALESCE(a.round, 0) = COALESCE(:round, 0) " +
           "AND a.attendanceDate = :date AND a.checkOut = :checkOut")
    boolean existsDuplicateExit(@Param("name") String studentName,
                                @Param("courseName") String courseName,
                                @Param("round") Integer round,
                                @Param("date") LocalDate attendanceDate,
                                @Param("checkOut") String checkOut);

    /** 취소 — 특정 학생의 당일 기록 전체 삭제 (수동 취소용) */
    @Modifying
    @Query("DELETE FROM AttendanceLogEntity a " +
           "WHERE a.studentName = :name AND a.courseName = :courseName " +
           "AND COALESCE(a.round, 0) = COALESCE(:round, 0) AND a.attendanceDate = :date")
    int deleteStudentDay(@Param("name") String studentName,
                         @Param("courseName") String courseName,
                         @Param("round") Integer round,
                         @Param("date") LocalDate attendanceDate);
}
