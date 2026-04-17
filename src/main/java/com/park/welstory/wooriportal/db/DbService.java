package com.park.welstory.wooriportal.db;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class DbService {

    private final JdbcTemplate jdbcTemplate;
    private static final List<String> SYSTEM_SCHEMAS = Arrays.asList("information_schema", "mysql", "performance_schema", "sys");

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    /** JDBC URL에서 host, port 파싱 */
    private String[] parseHostPort() {
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
        String sql = String.format("SELECT * FROM `%s`.`%s` LIMIT 100", schemaName, tableName);
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getDbUsers(String schemaName) {
        String sql = "SELECT User, Host FROM mysql.db WHERE Db = ? OR Db = '*'";
        try {
            return jdbcTemplate.queryForList(sql, schemaName);
        } catch (Exception e) {
            System.err.println("Error accessing mysql.db table: " + e.getMessage());
            return jdbcTemplate.queryForList("SELECT USER() as User, 'localhost' as Host");
        }
    }

    public void addPrivilege(String user, String host, String schema, String table, String privilege) {
        String sql = String.format("GRANT %s ON `%s`.`%s` TO '%s'@'%s'", privilege, schema, table, user, host);
        jdbcTemplate.execute(sql);
        jdbcTemplate.execute("FLUSH PRIVILEGES");
    }

    public void revokePrivilege(String user, String host, String schema, String table, String privilege) {
        String sql = String.format("REVOKE %s ON `%s`.`%s` FROM '%s'@'%s'", privilege, schema, table, user, host);
        jdbcTemplate.execute(sql);
        jdbcTemplate.execute("FLUSH PRIVILEGES");
    }

    public void deleteData(String schema, String table, String primaryKeyColumn, Object value) {
        String sql = String.format("DELETE FROM `%s`.`%s` WHERE `%s` = ?", schema, table, primaryKeyColumn);
        jdbcTemplate.update(sql, value);
    }

    public void dropSchema(String schemaName) {
        String sql = String.format("DROP DATABASE `%s`", schemaName);
        jdbcTemplate.execute(sql);
    }

    /**
     * 스키마(데이터베이스)를 생성합니다.
     */
    public void createSchema(String schemaName) {
        String sql = String.format("CREATE DATABASE `%s`", schemaName);
        jdbcTemplate.execute(sql);
    }

    /**
     * DB 사용자를 생성하고 지정 스키마에 ALL PRIVILEGES를 부여합니다.
     */
    public void createUser(String user, String host, String password, String targetSchema) {
        jdbcTemplate.execute(
                String.format("CREATE USER '%s'@'%s' IDENTIFIED BY '%s'", user, host, password)
        );
        jdbcTemplate.execute(
                String.format("GRANT ALL PRIVILEGES ON `%s`.* TO '%s'@'%s'", targetSchema, user, host)
        );
        jdbcTemplate.execute("FLUSH PRIVILEGES");
    }

    /**
     * DB 사용자를 삭제합니다.
     */
    public void dropUser(String user, String host) {
        jdbcTemplate.execute(String.format("DROP USER '%s'@'%s'", user, host));
        jdbcTemplate.execute("FLUSH PRIVILEGES");
    }

    /**
     * 특정 DB 사용자의 스키마 수준 권한을 GRANT 또는 REVOKE 합니다.
     */
    public void updatePrivilege(String user, String host, String schema, String privilege, String action) {
        String sql;
        if ("revoke".equalsIgnoreCase(action)) {
            sql = String.format("REVOKE %s ON `%s`.* FROM '%s'@'%s'", privilege, schema, user, host);
        } else {
            sql = String.format("GRANT %s ON `%s`.* TO '%s'@'%s'", privilege, schema, user, host);
        }
        jdbcTemplate.execute(sql);
        jdbcTemplate.execute("FLUSH PRIVILEGES");
    }

    /**
     * 특정 DB 사용자의 스키마 및 테이블 수준 권한 목록을 조회합니다.
     */
    public List<Map<String, Object>> getUserPrivileges(String user, String host) {
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
            return Collections.emptyList();
        }
    }
}