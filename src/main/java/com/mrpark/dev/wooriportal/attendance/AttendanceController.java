package com.mrpark.dev.wooriportal.attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/coolapi")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ── 오토잇 에이전트 수신 ─────────────────────────────────────
    @PostMapping("/write")
    @ResponseBody
    public ResponseEntity<String> write(@RequestBody String logLine) {
        if (logLine == null || logLine.isBlank()) return ResponseEntity.ok("EMPTY");
        attendanceService.processLogLine(logLine);
        return ResponseEntity.ok("OK");
    }

    // ── 수동 입력 (QR / 비컨) ───────────────────────────────────
    @PostMapping("/attendance/manual")
    @ResponseBody
    public ResponseEntity<String> manual(@RequestBody ManualAttendanceRequest req) {
        if (req.getCourseName() == null || req.getStudentName() == null || req.getType() == null) {
            return ResponseEntity.badRequest().body("MISSING_FIELDS");
        }
        attendanceService.processManual(req);
        return ResponseEntity.ok("OK");
    }

    // ── 출결 현황 뷰 ─────────────────────────────────────────────
    @GetMapping("/view")
    public String view() {
        return "attendance/view";
    }

    /** 과정+학생+상태 조합 데이터 (뷰 폴링용) */
    @GetMapping("/attendance/data")
    @ResponseBody
    public ResponseEntity<List<CourseAttendanceDTO>> getAttendanceData(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(attendanceService.getAttendanceData(target));
    }

    /** 학생 당일 기록 전체 (이름 클릭 모달용) */
    @GetMapping("/attendance/history")
    @ResponseBody
    public ResponseEntity<List<AttendanceLogDTO>> getStudentLog(
            @RequestParam String courseName,
            @RequestParam String studentName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(attendanceService.getStudentLog(courseName, target, studentName));
    }

    /** 원시 로그 목록 (구형 API 호환) */
    @GetMapping("/attendance")
    @ResponseBody
    public ResponseEntity<List<AttendanceLogDTO>> getAttendance(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(attendanceService.getByDate(target));
    }

    // ── 과정/학생 관리 뷰 ───────────────────────────────────────
    @GetMapping("/manage")
    public String manage() {
        return "attendance/manage";
    }

    // ── 과정 CRUD ───────────────────────────────────────────────
    @GetMapping("/courses")
    @ResponseBody
    public ResponseEntity<List<CourseDTO>> getCourses() {
        return ResponseEntity.ok(attendanceService.getAllCourses());
    }

    @PostMapping("/courses")
    @ResponseBody
    public ResponseEntity<CourseDTO> createCourse(@RequestBody CourseDTO dto) {
        return ResponseEntity.ok(attendanceService.createCourse(dto));
    }

    @PutMapping("/courses/{id}")
    @ResponseBody
    public ResponseEntity<CourseDTO> updateCourse(@PathVariable Long id,
                                                  @RequestBody CourseDTO dto) {
        return ResponseEntity.ok(attendanceService.updateCourse(id, dto));
    }

    @DeleteMapping("/courses/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        attendanceService.deleteCourse(id);
        return ResponseEntity.ok().build();
    }

    // ── 학생 CRUD ───────────────────────────────────────────────
    @GetMapping("/courses/{courseId}/students")
    @ResponseBody
    public ResponseEntity<List<StudentDTO>> getStudents(@PathVariable Long courseId) {
        return ResponseEntity.ok(attendanceService.getStudentsByCourse(courseId));
    }

    @PostMapping("/courses/{courseId}/students")
    @ResponseBody
    public ResponseEntity<StudentDTO> createStudent(@PathVariable Long courseId,
                                                    @RequestBody StudentDTO dto) {
        return ResponseEntity.ok(attendanceService.createStudent(courseId, dto));
    }

    @PutMapping("/students/{id}")
    @ResponseBody
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id,
                                                    @RequestBody StudentDTO dto) {
        return ResponseEntity.ok(attendanceService.updateStudent(id, dto));
    }

    @DeleteMapping("/students/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        attendanceService.deleteStudent(id);
        return ResponseEntity.ok().build();
    }
}
