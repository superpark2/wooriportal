package com.mrpark.dev.wooriportal.attendance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceLogRepository repository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    // ═══════════════════════════════════════════════════════════
    // 오토잇 수신 처리 — 항상 INSERT (덮어쓰기 없음)
    // ─ 4필드: name||course||checkIn||checkOut
    // ─ 5필드: cardNum(무시)||name||course||checkIn||checkOut
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public void processLogLine(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) return;

        try {
            int colonIdx = rawLine.lastIndexOf(": ");
            if (colonIdx < 0) return;
            String data = rawLine.substring(colonIdx + 2).trim();

            String[] parts = data.split("\\|\\|");

            String name, courseName, checkIn, checkOut;

            if (parts.length >= 5) {
                // 5필드 형식: cardNum(무시) || name || course || checkIn || checkOut
                name       = parts[1].trim();
                courseName = resolveCourseName(parts[2].trim());
                checkIn    = parts[3].trim();
                checkOut   = parts[4].trim();
            } else if (parts.length == 4) {
                // 4필드 형식: name || course || checkIn || checkOut
                name       = parts[0].trim();
                courseName = resolveCourseName(parts[1].trim());
                checkIn    = parts[2].trim();
                checkOut   = parts[3].trim();
            } else {
                log.warn("[출결] 파싱 불가 (필드 수 {}): {}", parts.length, rawLine);
                return;
            }

            if (name.isBlank() || courseName.isBlank()) return;

            LocalDate today  = LocalDate.now();
            boolean   isExit = !"0000".equals(checkOut) && !checkOut.isBlank();

            // ── 중복 체크 ──────────────────────────────────────
            if (isExit) {
                if (repository.existsDuplicateExit(name, courseName, today, checkOut)) {
                    log.debug("[출결] 중복 skip — 퇴실: {} / {} / {}", name, courseName, checkOut);
                    return;
                }
            } else {
                if (repository.existsDuplicateEntry(name, courseName, today, checkIn)) {
                    log.debug("[출결] 중복 skip — 입실: {} / {} / {}", name, courseName, checkIn);
                    return;
                }
            }

            AttendanceLogEntity e = new AttendanceLogEntity();
            e.setStudentName(name);
            e.setCourseName(courseName);
            e.setAttendanceDate(today);

            if (isExit) {
                // 퇴실 이벤트: checkIn이 "0000"이면 null로 저장 (집계 시 오염 방지)
                e.setCheckIn("0000".equals(checkIn) ? null : checkIn);
                e.setCheckOut(checkOut);
                log.info("[출결] 퇴실 기록: {} / {} / {}", name, courseName, checkOut);
            } else {
                e.setCheckIn(checkIn);
                log.info("[출결] 입실 기록: {} / {} / {}", name, courseName, checkIn);
            }

            repository.save(e);

        } catch (Exception ex) {
            log.error("[출결] 파싱 오류 - 원본: {}", rawLine, ex);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 수동 입력 처리 (QR / 비컨)
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public void processManual(ManualAttendanceRequest req) {
        String time = req.getTime() == null ? "" : req.getTime().replace(":", "");
        if (time.isBlank()) {
            // 기본값: 현재 시간
            java.time.LocalTime now = java.time.LocalTime.now();
            time = String.format("%02d%02d", now.getHour(), now.getMinute());
        }

        LocalDate today = LocalDate.now();
        boolean isExit  = "EXIT".equalsIgnoreCase(req.getType());

        AttendanceLogEntity e = new AttendanceLogEntity();
        e.setStudentName(req.getStudentName().trim());
        e.setCourseName(req.getCourseName().trim());
        e.setAttendanceDate(today);

        if (isExit) {
            e.setCheckOut(time);
            log.info("[수동] 퇴실: {} / {} / {}", req.getStudentName(), req.getCourseName(), time);
        } else {
            e.setCheckIn(time);
            log.info("[수동] 입실: {} / {} / {}", req.getStudentName(), req.getCourseName(), time);
        }

        repository.save(e);
    }

    // ═══════════════════════════════════════════════════════════
    // 출결 현황 — 다중 레코드 집계 후 상태 계산
    // ═══════════════════════════════════════════════════════════

    public List<CourseAttendanceDTO> getAttendanceData(LocalDate date) {
        int dow = date.getDayOfWeek().getValue();
        String dowStr = String.valueOf(dow);

        List<CourseEntity> courses = courseRepository.findByActiveTrueOrderByCourseNameAsc()
                .stream()
                .filter(c -> Arrays.asList(c.getDaysOfWeek().split(",")).contains(dowStr))
                .filter(c -> (c.getStartDate() == null || !date.isBefore(c.getStartDate())))
                .filter(c -> (c.getEndDate() == null || !date.isAfter(c.getEndDate())))
                .collect(Collectors.toList());

        List<CourseAttendanceDTO> result = new ArrayList<>();

        for (CourseEntity course : courses) {
            List<StudentEntity> students =
                    studentRepository.findByCourseIdAndActiveTrueOrderByStudentNameAsc(course.getId());

            // 당일 전체 로그 → 학생 이름별 그룹핑
            Map<String, List<AttendanceLogEntity>> byName = new HashMap<>();
            repository.findByCourseNameAndAttendanceDate(course.getCourseName(), date)
                      .forEach(l -> byName.computeIfAbsent(l.getStudentName(), k -> new ArrayList<>()).add(l));

            List<CourseAttendanceDTO.StudentAttendanceItem> items = new ArrayList<>();
            for (StudentEntity student : students) {
                List<AttendanceLogEntity> logs =
                        byName.getOrDefault(student.getStudentName(), Collections.emptyList());

                // 집계: 유효한 가장 이른 입실 / 가장 늦은 퇴실
                // "0000" 은 퇴실 이벤트의 더미값이므로 제외
                String effectiveCheckIn = logs.stream()
                        .map(AttendanceLogEntity::getCheckIn)
                        .filter(Objects::nonNull)
                        .filter(t -> !"0000".equals(t))
                        .min(Comparator.naturalOrder()).orElse(null);
                String effectiveCheckOut = logs.stream()
                        .map(AttendanceLogEntity::getCheckOut)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder()).orElse(null);

                AttendanceLogEntity agg = null;
                if (!logs.isEmpty()) {
                    agg = new AttendanceLogEntity();
                    agg.setCheckIn(effectiveCheckIn);
                    agg.setCheckOut(effectiveCheckOut);
                }

                AttendanceStatus status = calcStatus(agg, course);

                // 표시 이름: 생년월일이 있으면 "홍길동 (990101)" 형식
                String displayName = student.getStudentName();
                if (student.getBirthPrefix() != null && !student.getBirthPrefix().isBlank()) {
                    displayName += " (" + student.getBirthPrefix() + ")";
                }

                items.add(new CourseAttendanceDTO.StudentAttendanceItem(
                        student.getId(),
                        displayName,
                        formatTime(effectiveCheckIn),
                        formatTime(effectiveCheckOut),
                        status.name(),
                        statusLabel(status),
                        colorClass(status)
                ));
            }

            result.add(new CourseAttendanceDTO(
                    course.getId(),
                    course.getCourseName(),
                    formatTime(course.getCheckInTime()),
                    formatTime(course.getCheckOutTime()),
                    items
            ));
        }

        return result;
    }

    /** 학생 당일 기록 전체 (이름 클릭 모달용) */
    public List<AttendanceLogDTO> getStudentLog(String courseName, LocalDate date, String studentName) {
        // 이름에 "(생년월일)" 접미어가 붙어있을 수 있으므로 괄호 앞까지만 추출
        String pureName = studentName.replaceAll("\\s*\\(.*\\)\\s*$", "").trim();
        return repository.findByCourseNameAndAttendanceDateAndStudentNameOrderByCreatedAtAsc(
                        courseName, date, pureName)
                .stream().map(AttendanceLogDTO::from).collect(Collectors.toList());
    }

    /** 날짜별 원시 로그 (구형 호환) */
    public List<AttendanceLogDTO> getByDate(LocalDate date) {
        return repository.findByAttendanceDateOrderByCreatedAtAsc(date)
                         .stream().map(AttendanceLogDTO::from).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // 과정 CRUD
    // ═══════════════════════════════════════════════════════════

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAllByOrderByCourseNameAsc()
                               .stream().map(CourseDTO::from).collect(Collectors.toList());
    }

    @Transactional
    public CourseDTO createCourse(CourseDTO dto) {
        CourseEntity e = new CourseEntity();
        dto.applyTo(e);
        return CourseDTO.from(courseRepository.save(e));
    }

    @Transactional
    public CourseDTO updateCourse(Long id, CourseDTO dto) {
        CourseEntity e = courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + id));
        dto.applyTo(e);
        return CourseDTO.from(courseRepository.save(e));
    }

    @Transactional
    public void deleteCourse(Long id) {
        CourseEntity e = courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + id));
        e.setActive(false);
        courseRepository.save(e);
    }

    // ═══════════════════════════════════════════════════════════
    // 학생 CRUD
    // ═══════════════════════════════════════════════════════════

    public List<StudentDTO> getStudentsByCourse(Long courseId) {
        return studentRepository.findByCourseIdOrderByStudentNameAsc(courseId)
                                .stream().map(StudentDTO::from).collect(Collectors.toList());
    }

    @Transactional
    public StudentDTO createStudent(Long courseId, StudentDTO dto) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));
        StudentEntity e = new StudentEntity();
        e.setCourse(course);
        e.setStudentName(dto.getStudentName());
        e.setBirthPrefix(dto.getBirthPrefix() != null && !dto.getBirthPrefix().isBlank()
                ? dto.getBirthPrefix().trim() : null);
        e.setActive(true);
        return StudentDTO.from(studentRepository.save(e));
    }

    @Transactional
    public StudentDTO updateStudent(Long id, StudentDTO dto) {
        StudentEntity e = studentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Student not found: " + id));
        e.setStudentName(dto.getStudentName());
        e.setBirthPrefix(dto.getBirthPrefix() != null && !dto.getBirthPrefix().isBlank()
                ? dto.getBirthPrefix().trim() : null);
        e.setActive(dto.isActive());
        return StudentDTO.from(studentRepository.save(e));
    }

    @Transactional
    public void deleteStudent(Long id) {
        StudentEntity e = studentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Student not found: " + id));
        e.setActive(false);
        studentRepository.save(e);
    }

    // ═══════════════════════════════════════════════════════════
    // 과정명 정규화 매칭
    //  1순위: 공백 제거 후 완전 일치
    //  2순위: 공백 제거 후 Levenshtein 유사도 90% 이상 → 가장 높은 것 선택
    //  미매칭: 원본 그대로 (로그 경고)
    // ═══════════════════════════════════════════════════════════

    private String resolveCourseName(String raw) {
        String normalizedRaw = raw.replaceAll("\\s+", "");

        List<CourseEntity> courses = courseRepository.findByActiveTrueOrderByCourseNameAsc();

        // 1순위: 공백 무시 완전 일치
        for (CourseEntity c : courses) {
            if (c.getCourseName().replaceAll("\\s+", "").equals(normalizedRaw)) {
                if (!c.getCourseName().equals(raw)) {
                    log.info("[과정명] 공백 정규화: '{}' → '{}'", raw, c.getCourseName());
                }
                return c.getCourseName();
            }
        }

        // 2순위: 유사도 90% 이상 중 최고 점수
        String bestMatch = null;
        double bestScore = 0.0;
        for (CourseEntity c : courses) {
            double score = nameSimilarity(normalizedRaw, c.getCourseName().replaceAll("\\s+", ""));
            if (score > bestScore) {
                bestScore = score;
                bestMatch = c.getCourseName();
            }
        }

        if (bestScore >= 0.9 && bestMatch != null) {
            log.info("[과정명] 유사 매칭({:.1f}%): '{}' → '{}'", bestScore * 100, raw, bestMatch);
            return bestMatch;
        }

        log.warn("[과정명] 매칭 실패 — 원본 저장: '{}'", raw);
        return raw;
    }

    /** 레벤슈타인 기반 유사도 (0.0 ~ 1.0) */
    private double nameSimilarity(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        int maxLen = Math.max(a.length(), b.length());
        return 1.0 - (double) levenshtein(a, b) / maxLen;
    }

    private int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[m][n];
    }

    // ═══════════════════════════════════════════════════════════
    // 내부 유틸리티
    // ═══════════════════════════════════════════════════════════

    private AttendanceStatus calcStatus(AttendanceLogEntity log, CourseEntity course) {
        if (log == null) return AttendanceStatus.ABSENT;

        String checkIn  = log.getCheckIn();
        String checkOut = log.getCheckOut();
        String ciTime   = course.getCheckInTime();
        String coTime   = course.getCheckOutTime();

        if (ciTime == null || ciTime.isBlank()) {
            return (checkOut == null) ? AttendanceStatus.PRESENT : AttendanceStatus.EXITED;
        }

        String lateThreshold = addMinutes(ciTime, course.getLateGraceMinutes());
        boolean isLate       = checkIn != null && checkIn.compareTo(lateThreshold) > 0;
        boolean hasExited    = checkOut != null;
        boolean isEarlyLeave = hasExited && coTime != null && !coTime.isBlank()
                               && checkOut.compareTo(coTime) < 0;

        if (!isLate && !hasExited)                 return AttendanceStatus.PRESENT;
        if (!isLate && hasExited && !isEarlyLeave) return AttendanceStatus.EXITED;
        if (!isLate && hasExited && isEarlyLeave)  return AttendanceStatus.EARLY_LEAVE;
        if (isLate  && !hasExited)                 return AttendanceStatus.LATE;
        if (isLate  && hasExited && !isEarlyLeave) return AttendanceStatus.LATE_EXITED;
        return AttendanceStatus.LATE_EARLY_LEAVE;
    }

    private String addMinutes(String hhmm, int minutes) {
        if (hhmm == null || hhmm.length() != 4 || minutes <= 0) return hhmm;
        int h = Integer.parseInt(hhmm.substring(0, 2));
        int m = Integer.parseInt(hhmm.substring(2));
        int total = h * 60 + m + minutes;
        return String.format("%02d%02d", total / 60, total % 60);
    }

    static String formatTime(String hhmm) {
        if (hhmm == null || hhmm.length() != 4) return hhmm;
        return hhmm.substring(0, 2) + ":" + hhmm.substring(2);
    }

    private String statusLabel(AttendanceStatus s) {
        return switch (s) {
            case ABSENT           -> "미출석";
            case PRESENT          -> "출석";       // ← "재원중" 에서 변경
            case EXITED           -> "퇴실";
            case LATE             -> "지각";
            case LATE_EXITED      -> "지각·퇴실";
            case EARLY_LEAVE      -> "조퇴";
            case LATE_EARLY_LEAVE -> "지각·조퇴";
        };
    }

    private String colorClass(AttendanceStatus s) {
        return switch (s) {
            case ABSENT  -> "absent";
            case PRESENT -> "present";
            case EXITED  -> "exited";
            default      -> "late";
        };
    }
}
