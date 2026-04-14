package com.park.welstory.wooriportal.db;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class DbService {

    private final JdbcTemplate jdbcTemplate;
    // 제외할 시스템 데이터베이스 목록
    private static final List<String> SYSTEM_SCHEMAS = Arrays.asList("information_schema", "mysql", "performance_schema", "sys");

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    /** JDBC URL에서 host, port 파싱 */
    private String[] parseHostPort() {
        // jdbc:mysql://host:3306/db?params 또는 jdbc:mariadb://host:3306/db
        String stripped = datasourceUrl.replaceFirst("jdbc:(mysql|mariadb)://", "");
        String hostPort = stripped.split("/")[0].split("\\?")[0];
        String host = hostPort.contains(":") ? hostPort.split(":")[0] : hostPort;
        String port = hostPort.contains(":") ? hostPort.split(":")[1] : "3306";
        return new String[]{host, port};
    }

    /**
     * mysqldump로 스키마 전체를 덤프하여 바이트 배열로 반환
     */
    public byte[] dumpSchema(String schemaName) throws Exception {
        String[] hp = parseHostPort();
        ProcessBuilder pb = new ProcessBuilder(
                "mysqldump",
                "-h", hp[0], "-P", hp[1],
                "-u", datasourceUsername,
                "-p" + datasourcePassword,
                "--single-transaction", "--routines", "--triggers",
                schemaName
        );
        pb.redirectErrorStream(false);
        Process process = pb.start();

        byte[] output = process.getInputStream().readAllBytes();
        byte[] errOutput = process.getErrorStream().readAllBytes();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(new String(errOutput));
        }
        return output;
    }

    /**
     * .sql 파일 스트림을 mysql CLI로 파이핑하여 스키마에 복원
     */
    public void restoreSchema(String schemaName, InputStream sqlStream) throws Exception {
        String[] hp = parseHostPort();
        ProcessBuilder pb = new ProcessBuilder(
                "mysql",
                "-h", hp[0], "-P", hp[1],
                "-u", datasourceUsername,
                "-p" + datasourcePassword,
                schemaName
        );
        Process process = pb.start();

        try (OutputStream stdin = process.getOutputStream()) {
            sqlStream.transferTo(stdin);
        }

        byte[] errOutput = process.getErrorStream().readAllBytes();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(new String(errOutput));
        }
    }


    public List<String> getAllSchemas() {
        String sql = "SHOW DATABASES";
        List<String> allSchemas = jdbcTemplate.queryForList(sql, String.class);
        // 시스템 스키마를 제외하고 필터링
        return allSchemas.stream()
                .filter(schema -> !SYSTEM_SCHEMAS.contains(schema.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<String> getTablesInSchema(String schemaName) {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
        return jdbcTemplate.queryForList(sql, String.class, schemaName);
    }

    public List<String> getColumnNames(String schemaName, String tableName) {
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        return jdbcTemplate.queryForList(sql, String.class, schemaName, tableName);
    }

    public List<Map<String, Object>> getTableData(String schemaName, String tableName) {
        // 테이블 이름과 스키마 이름은 백틱(`)으로 감싸서 SQL Injection 방지 및 특수문자 대응
        String sql = String.format("SELECT * FROM `%s`.`%s` LIMIT 100", schemaName, tableName);
        return jdbcTemplate.queryForList(sql);
    }

    // --- 관리자 계정 및 권한 관리 (예시: DB 사용자 기준) ---

    public List<Map<String, Object>> getDbUsers(String schemaName) {
        // 특정 스키마에 권한이 있는 사용자 목록 조회 (MariaDB/MySQL 기준)
        // 이 쿼리는 'mysql.db' 테이블에 대한 접근 권한이 필요합니다.
        // 권한이 없을 경우 예외가 발생할 수 있습니다.
        String sql = "SELECT User, Host FROM mysql.db WHERE Db = ? OR Db = '*'";
        try {
            return jdbcTemplate.queryForList(sql, schemaName);
        } catch (Exception e) {
            // mysql.db 접근 권한이 없을 경우 현재 세션 사용자 정보라도 반환하거나 빈 리스트 반환
            // 실제 운영 환경에서는 적절한 예외 처리 또는 권한 부여가 필요합니다.
            System.err.println("Error accessing mysql.db table: " + e.getMessage());
            return jdbcTemplate.queryForList("SELECT USER() as User, 'localhost' as Host");
        }
    }

    public void addPrivilege(String user, String host, String schema, String table, String privilege) {
        // 특정 사용자에게 특정 테이블 권한 부여 (GRANT)
        String sql = String.format("GRANT %s ON `%s`.`%s` TO '%s'@'%s'", privilege, schema, table, user, host);
        jdbcTemplate.execute(sql);
        jdbcTemplate.execute("FLUSH PRIVILEGES");
    }

    public void revokePrivilege(String user, String host, String schema, String table, String privilege) {
        // 권한 회수 (REVOKE)
        String sql = String.format("REVOKE %s ON `%s`.`%s` FROM '%s'@'%s'", privilege, schema, table, user, host);
        jdbcTemplate.execute(sql);
        jdbcTemplate.execute("FLUSH PRIVILEGES");
    }

    public void deleteData(String schema, String table, String primaryKeyColumn, Object value) {
        String sql = String.format("DELETE FROM `%s`.`%s` WHERE `%s` = ?", schema, table, primaryKeyColumn);
        jdbcTemplate.update(sql, value);
    }

    /**
     * 지정된 스키마(데이터베이스)를 삭제합니다.
     * @param schemaName 삭제할 스키마 이름
     */
    public void dropSchema(String schemaName) {
        // 스키마 이름은 백틱으로 감싸서 SQL Injection 방지 및 특수문자 대응
        String sql = String.format("DROP DATABASE `%s`", schemaName);
        jdbcTemplate.execute(sql);
    }

    /**
     * 특정 DB 사용자의 스키마 및 테이블 수준 권한 목록을 조회합니다.
     * @param user DB 사용자명
     * @param host DB 호스트
     */
    public List<Map<String, Object>> getUserPrivileges(String user, String host) {
        // GRANTEE 형식: 'user'@'host' (따옴표 포함)
        String grantee = String.format("'%s'@'%s'", user, host);
        String sql =
                "SELECT 'SCHEMA' AS level, TABLE_SCHEMA AS `schema`, '*' AS `table`, PRIVILEGE_TYPE AS privilege " +
                        "FROM information_schema.SCHEMA_PRIVILEGES WHERE GRANTEE = ? " +
                        "UNION ALL " +
                        "SELECT 'TABLE', TABLE_SCHEMA, TABLE_NAME, PRIVILEGE_TYPE " +
                        "FROM information_schema.TABLE_PRIVILEGES WHERE GRANTEE = ? " +
                        "ORDER BY level DESC, `schema`, `table`, privilege";
        try {
            return jdbcTemplate.queryForList(sql, grantee, grantee);
        } catch (Exception e) {
            System.err.println("Error fetching user privileges: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}