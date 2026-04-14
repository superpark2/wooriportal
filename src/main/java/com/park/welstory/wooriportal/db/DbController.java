package com.park.welstory.wooriportal.db;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@RequestMapping("/db")
public class DbController {

    private final DbService dbService;

    @Value("${db.admin.id}")
    private String adminId;

    @Value("${db.admin.pw}")
    private String adminPw;

    @GetMapping
    public String dbManagementPage(
            @RequestParam(value = "schema", required = false) String selectedSchema,
            @RequestParam(value = "table", required = false) String selectedTable,
            @RequestParam(value = "view", defaultValue = "data") String view,
            Model model) {

        if (selectedSchema == null) {
            try {
                List<String> schemaNames = dbService.getAllSchemas();
                model.addAttribute("schemaNames", schemaNames);
                model.addAttribute("currentView", "schemas");
            } catch (Exception e) {
                model.addAttribute("error", "데이터베이스 스키마 목록을 불러오는 데 실패했습니다: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            model.addAttribute("selectedSchema", selectedSchema);
            model.addAttribute("activeView", view);

            // 권한 관리 모달의 스키마 드롭다운에 항상 전체 스키마 목록이 필요
            try {
                List<String> allSchemaNames = dbService.getAllSchemas();
                model.addAttribute("schemaNames", allSchemaNames);
            } catch (Exception e) {
                // 스키마 목록 로드 실패는 무시 (핵심 기능 아님)
            }

            try {
                List<String> tableNames = dbService.getTablesInSchema(selectedSchema);
                model.addAttribute("tableNames", tableNames);
                model.addAttribute("currentView", "tables");

                List<Map<String, Object>> dbUsers = dbService.getDbUsers(selectedSchema);
                model.addAttribute("dbUsers", dbUsers);

            } catch (Exception e) {
                model.addAttribute("error", "스키마 '" + selectedSchema + "' 정보를 불러오는 데 실패했습니다: " + e.getMessage());
                e.printStackTrace();
            }

            // 테이블 데이터는 별도 try-catch: 손상된 테이블 하나가 스키마 전체 로드를 막지 않도록
            if ("data".equals(view) && selectedTable != null) {
                model.addAttribute("selectedTable", selectedTable);
                try {
                    List<String> columnNames = dbService.getColumnNames(selectedSchema, selectedTable);
                    model.addAttribute("columnNames", columnNames);

                    List<Map<String, Object>> tableData = dbService.getTableData(selectedSchema, selectedTable);
                    model.addAttribute("tableData", tableData);
                    model.addAttribute("tableDataMessage", "테이블 '" + selectedTable + "'의 상위 100개 데이터입니다.");
                } catch (Exception e) {
                    String msg = e.getMessage() != null && e.getMessage().contains("1030")
                            ? "테이블스페이스 파일(.ibd)이 유실된 손상된 테이블입니다. DB 관리자에게 문의하세요."
                            : "테이블 데이터를 불러올 수 없습니다: " + e.getMessage();
                    model.addAttribute("tableError", msg);
                    e.printStackTrace();
                }
            }
        }
        return "db/db";
    }

    /**
     * 스키마(데이터베이스)를 삭제하는 엔드포인트
     */
    @PostMapping("/dropSchema")
    public String dropSchema(
            @RequestParam("schemaName") String schemaName,
            @RequestParam("adminId") String adminId_param,
            @RequestParam("adminPw") String adminPw_param,
            RedirectAttributes redirectAttributes) {

        // TODO: 실제 프로젝트의 인증 정보와 연동하여 검증을 강화하세요.
        if (!adminId.equals(adminId_param) || !adminPw.equals(adminPw_param)) {
            redirectAttributes.addFlashAttribute("error", "관리자 인증 정보가 올바르지 않습니다.");
            return "redirect:/db";
        }

        try {
            dbService.dropSchema(schemaName);
            redirectAttributes.addFlashAttribute("success", "스키마 '" + schemaName + "'가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "스키마 삭제 중 오류가 발생했습니다: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/db";
    }

    /**
     * 관리자 인증 확인 (열람 잠금 해제용)
     */
    @PostMapping("/verifyAdmin")
    @ResponseBody
    public Map<String, Object> verifyAdmin(
            @RequestParam("adminId") String adminId_param,
            @RequestParam("adminPw") String adminPw_param) {
        boolean ok = adminId.equals(adminId_param) && adminPw.equals(adminPw_param);
        return Map.of("ok", ok);
    }

    /**
     * 스키마 복원 — 업로드된 .sql 파일을 mysql CLI로 실행
     */
    @PostMapping("/restore")
    @ResponseBody
    public Map<String, Object> restoreSchema(
            @RequestParam("schemaName") String schemaName,
            @RequestParam("adminId") String adminId_param,
            @RequestParam("adminPw") String adminPw_param,
            @RequestParam("sqlFile") MultipartFile sqlFile) {

        if (!adminId.equals(adminId_param) || !adminPw.equals(adminPw_param)) {
            return Map.of("ok", false, "message", "관리자 인증 정보가 올바르지 않습니다.");
        }

        if (sqlFile == null || sqlFile.isEmpty()) {
            return Map.of("ok", false, "message", ".sql 파일이 비어 있습니다.");
        }

        try (java.io.InputStream is = sqlFile.getInputStream()) {
            dbService.restoreSchema(schemaName, is);
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "message", "복원 실패: " + e.getMessage());
        }
    }

    /**
     * 스키마 백업 — mysqldump 결과를 .sql 파일로 다운로드
     */
    @GetMapping("/dump")
    public ResponseEntity<byte[]> dumpSchema(
            @RequestParam("schema") String schemaName,
            @RequestParam("adminId") String adminId_param,
            @RequestParam("adminPw") String adminPw_param) {

        if (!adminId.equals(adminId_param) || !adminPw.equals(adminPw_param)) {
            return ResponseEntity.status(403)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("관리자 인증 정보가 올바르지 않습니다.".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try {
            byte[] sqlData = dbService.dumpSchema(schemaName);
            String filename = schemaName + "_backup_"
                    + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + ".sql";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(sqlData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("백업 실패: " + e.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /**
     */
    @GetMapping("/userPrivileges")
    @ResponseBody
    public List<Map<String, Object>> getUserPrivileges(
            @RequestParam("user") String user,
            @RequestParam("host") String host) {
        return dbService.getUserPrivileges(user, host);
    }
}