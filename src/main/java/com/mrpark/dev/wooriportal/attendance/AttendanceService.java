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

            String name, rawCourse, checkIn, checkOut;
            int round = 0;

            if (parts.length >= 9) {
                // 출결전송 선택 형식: card||date||AIG(무시)||course||memberNo||name||checkIn||checkOut||회차
                rawCourse = parts[3].trim();
                name      = parts[5].trim();
                checkIn   = parts[6].trim();
                checkOut  = parts[7].trim();
                round     = parseRound(parts[8]);
            } else if (parts.length >= 5) {
                // 구형 5필드: cardNum(무시) || name || course || checkIn || checkOut
                name      = parts[1].trim();
                rawCourse = parts[2].trim();
                checkIn   = parts[3].trim();
                checkOut  = parts[4].trim();
            } else if (parts.length == 4) {
                // 구형 4필드: name || course || checkIn || checkOut
                name      = parts[0].trim();
                rawCourse = parts[1].trim();
                checkIn   = parts[2].trim();
                checkOut  = parts[3].trim();
            } else {
                log.warn("[출결] 파싱 불가 (필드 수 {}): {}", parts.length, rawLine);
                return;
            }

            if (name.isBlank() || rawCourse.isBlank()) return;

            // 미등록 과정/학생은 기본 틀로 자동 등록 (개강·종강·지각시간 등은 추후 수동 보정)
            CourseEntity course = resolveOrCreateCourse(rawCourse, round);
            String courseName   = course.getCourseName();
            ensureStudent(course, name);

            LocalDate today  = LocalDate.now();
            boolean   isExit = !"0000".equals(checkOut) && !checkOut.isBlank();

            // ── 중복 체크 ──────────────────────────────────────
            if (isExit) {
                if (repository.existsDuplicateExit(name, courseName, round, today, checkOut)) {
                    log.debug("[출결] 중복 skip — 퇴실: {} / {} / {} / 회차 {}", name, courseName, checkOut, round);
                    return;
                }
            } else {
                if (repository.existsDuplicateEntry(name, courseName, round, today, checkIn)) {
                    log.debug("[출결] 중복 skip — 입실: {} / {} / {} / 회차 {}", name, courseName, checkIn, round);
                    return;
                }
            }

            AttendanceLogEntity e = new AttendanceLogEntity();
            e.setStudentName(name);
            e.setCourseName(courseName);
            e.setRound(round);
            e.setSource("HRD");
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
        int round       = req.getRound() != null ? req.getRound() : 0;
        String method   = (req.getMethod() == null || req.getMethod().isBlank())
                ? "MANUAL" : req.getMethod().trim().toUpperCase();

        // 미등록 과정/학생은 기본 틀로 자동 등록
        String name        = req.getStudentName().trim();
        CourseEntity course = resolveOrCreateCourse(req.getCourseName().trim(), round);
        ensureStudent(course, name);

        AttendanceLogEntity e = new AttendanceLogEntity();
        e.setStudentName(name);
        e.setCourseName(course.getCourseName());
        e.setRound(round);
        e.setSource(method);
        e.setAttendanceDate(today);

        if (isExit) {
            e.setCheckOut(time);
            log.info("[수동:{}] 퇴실: {} / {} / 회차 {} / {}", method, name, course.getCourseName(), round, time);
        } else {
            e.setCheckIn(time);
            log.info("[수동:{}] 입실: {} / {} / 회차 {} / {}", method, name, course.getCourseName(), round, time);
        }

        repository.save(e);
    }

    /** 취소 — 특정 학생의 당일 출결 기록 전체 삭제 */
    @Transactional
    public void cancelAttendance(String courseName, Integer round, String studentName) {
        int r = round != null ? round : 0;
        int deleted = repository.deleteStudentDay(studentName.trim(), courseName.trim(), r, LocalDate.now());
        log.info("[취소] {} / {} / 회차 {} — {}건 삭제", studentName, courseName, r, deleted);
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

            // 당일 전체 로그 → 학생 이름별 그룹핑 (회차로 오전/오후반 구분)
            Map<String, List<AttendanceLogEntity>> byName = new HashMap<>();
            repository.findByCourseAndDate(course.getCourseName(), course.getRound(), date)
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
                    course.getRound(),
                    course.getDaysOfWeek(),
                    formatTime(course.getCheckInTime()),
                    formatTime(course.getCheckOutTime()),
                    items
            ));
        }

        return result;
    }

    /** 학생 당일 기록 전체 (이름 클릭 모달용) */
    public List<AttendanceLogDTO> getStudentLog(String courseName, Integer round, LocalDate date, String studentName) {
        // 이름에 "(생년월일)" 접미어가 붙어있을 수 있으므로 괄호 앞까지만 추출
        String pureName = studentName.replaceAll("\\s*\\(.*\\)\\s*$", "").trim();
        return repository.findStudentLog(courseName, round, date, pureName)
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
    // 과정 식별(공백 제거 완전일치 + 회차) + 미등록 시 자동 등록
    //  - 같은 과정명이라도 회차(기수)가 다르면 별개 과정(오전/오후반)
    //  - 매칭: 공백 모두 제거한 과정명 완전일치 AND 회차 일치 (유사도 매칭 없음)
    // ═══════════════════════════════════════════════════════════

    private CourseEntity resolveOrCreateCourse(String raw, int round) {
        String normalizedRaw = stripSpaces(raw);

        List<CourseEntity> courses = courseRepository.findAllByOrderByCourseNameAsc();

        // 공백 무시 완전 일치 + 회차 일치 (동일 조건이 여러 개면 활성 과정 우선)
        CourseEntity match = null;
        for (CourseEntity c : courses) {
            int cRound = c.getRound() != null ? c.getRound() : 0;
            if (cRound == round && stripSpaces(c.getCourseName()).equals(normalizedRaw)) {
                if (match == null || (!match.isActive() && c.isActive())) match = c;
            }
        }

        if (match != null) {
            // 삭제(비활성)된 과정에 다시 출결이 들어오면 되살린다 (재전송 시 누락 복구)
            if (!match.isActive()) {
                match.setActive(true);
                courseRepository.save(match);
                log.info("[과정] 비활성 → 재활성화: '{}' (회차 {})", match.getCourseName(), round);
            }
            if (!match.getCourseName().equals(raw)) {
                log.info("[과정명] 공백 정규화: '{}' → '{}' (회차 {})", raw, match.getCourseName(), round);
            }
            return match;
        }

        // 미매칭 → 기본 틀로 자동 등록
        return createDefaultCourse(raw, round);
    }

    /** 미등록 과정을 최소 기본값으로 등록 (요일=월~금, 시간/기간은 미설정 → 추후 수동 보정) */
    private CourseEntity createDefaultCourse(String raw, int round) {
        CourseEntity c = new CourseEntity();
        c.setCourseName(raw.trim());
        c.setRound(round);
        c.setDaysOfWeek("1,2,3,4,5");   // 월~금 (관리 화면에서는 항상 보이며, 일별 뷰 노출 요일은 추후 보정)
        c.setActive(true);
        CourseEntity saved = courseRepository.save(c);
        log.info("[과정] 자동 등록(기본 틀): '{}' (회차 {}, id={})", saved.getCourseName(), round, saved.getId());
        return saved;
    }

    /** "17" 등 회차 문자열 → int (파싱 실패 시 0) */
    private static int parseRound(String s) {
        if (s == null) return 0;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    /** 과정 내 동일 이름 학생이 없으면 자동 등록, 비활성(삭제) 학생이면 되살린다 */
    private void ensureStudent(CourseEntity course, String studentName) {
        StudentEntity existing = studentRepository
                .findFirstByCourseIdAndStudentName(course.getId(), studentName)
                .orElse(null);

        if (existing != null) {
            // 삭제(비활성)된 학생에 다시 출결이 들어오면 되살린다 (재전송 시 누락 복구)
            if (!existing.isActive()) {
                existing.setActive(true);
                studentRepository.save(existing);
                log.info("[학생] 비활성 → 재활성화: '{}' / 과정 '{}'", studentName, course.getCourseName());
            }
            return;
        }

        StudentEntity s = new StudentEntity();
        s.setCourse(course);
        s.setStudentName(studentName);
        s.setActive(true);
        studentRepository.save(s);
        log.info("[학생] 자동 등록: '{}' / 과정 '{}'", studentName, course.getCourseName());
    }

    /**
     * 과정명 비교용 공백 제거 — 일반 공백/탭뿐 아니라 한글 데이터에 흔한
     * 전각 공백(U+3000)·비분리 공백(U+00A0)·제로폭 문자까지 모두 제거한다.
     * (Java 정규식의 \\s 는 전각 공백을 잡지 못해 매칭이 실패하는 문제 방지)
     */
    private static final java.util.regex.Pattern WS =
            java.util.regex.Pattern.compile("[\\s\\u00A0\\u200B\\u3000\\uFEFF]+");

    private static String stripSpaces(String s) {
        return s == null ? "" : WS.matcher(s).replaceAll("");
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
