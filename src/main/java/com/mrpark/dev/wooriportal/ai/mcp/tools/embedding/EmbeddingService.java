package com.mrpark.dev.wooriportal.ai.mcp.tools.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG(Retrieval-Augmented Generation) 임베딩 서비스.
 *
 * - pgvector PostgreSQL에 문서를 저장하고 유사도 검색을 수행한다.
 * - Ollama /api/embeddings 엔드포인트로 벡터를 생성한다.
 * - MCP EmbeddingTool과 어드민 AiController에서 공통 사용된다.
 *
 * application.properties 필수 설정:
 *   ollama.api.url=http://localhost:11434/api
 *   ollama.embedding.model=nomic-embed-text   (기본값 사용 가능)
 *   embedding.db.url=jdbc:postgresql://localhost:5432/vectordb
 *   embedding.db.username=postgres
 *   embedding.db.password=secret
 */
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    @Value("${ollama.api.url}")
    private String ollamaApiUrl;

    @Value("${ollama.embedding.model:nomic-embed-text}")
    private String embeddingModel;

    @Value("${embedding.db.url}")
    private String dbUrl;

    @Value("${embedding.db.username}")
    private String dbUsername;

    @Value("${embedding.db.password}")
    private String dbPassword;

    /** 긴 텍스트를 청크로 나눠 평균 벡터를 구할 때 사용하는 청크 크기 */
    private static final int CHUNK_SIZE = 1200;

    private final ObjectMapper mapper = new ObjectMapper();
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        DataSource ds = DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(dbUrl)
                .username(dbUsername)
                .password(dbPassword)
                .build();
        this.jdbcTemplate = new JdbcTemplate(ds);

        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS documents (
                        id        SERIAL PRIMARY KEY,
                        category  VARCHAR(100),
                        title     VARCHAR(255),
                        content   TEXT,
                        embedding vector(768)
                    )""");
        } catch (Exception e) {
            System.err.println("[EmbeddingService] 초기화 오류: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  공개 API
    // ══════════════════════════════════════════════════════════════

    /**
     * 쿼리와 가장 유사한 문서 최대 3건을 찾아 컨텍스트 문자열로 반환.
     * 결과가 없으면 빈 문자열 반환.
     */
    public String retrieveContext(String query) {
        if (query == null || query.isBlank()) return "";

        List<Double> queryVec = generateEmbedding(query);
        if (queryVec.isEmpty()) return "";

        PGvector pgVec = toVector(queryVec);

        // 카테고리 힌트가 쿼리에 포함된 경우 해당 카테고리를 우선 검색
        List<String> categories = jdbcTemplate.queryForList(
                "SELECT DISTINCT category FROM documents WHERE category IS NOT NULL",
                String.class);
        String detectedCategory = categories.stream()
                .filter(c -> query.contains(c))
                .findFirst()
                .orElse(null);

        String sql;
        Object[] params;
        if (detectedCategory != null) {
            sql    = "SELECT category, title, content FROM documents WHERE category = ? ORDER BY embedding <=> ? LIMIT 3";
            params = new Object[]{detectedCategory, pgVec};
        } else {
            sql    = "SELECT category, title, content FROM documents ORDER BY embedding <=> ? LIMIT 3";
            params = new Object[]{pgVec};
        }

        List<String> parts = new ArrayList<>();
        try {
            jdbcTemplate.query(sql, rs -> {
                parts.add("종류: " + rs.getString("category")
                        + "\n제목: " + rs.getString("title")
                        + "\n내용: " + rs.getString("content"));
            }, params);
        } catch (Exception e) {
            System.err.println("[EmbeddingService] 검색 오류: " + e.getMessage());
        }

        return String.join("\n\n---\n\n", parts);
    }

    public void addDocument(String category, String title, String content) {
        List<Double> vec = generateEmbedding(content);
        if (vec.isEmpty()) return;
        jdbcTemplate.update(
                "INSERT INTO documents (category, title, content, embedding) VALUES (?, ?, ?, ?)",
                category, title, content, toVector(vec));
    }

    public void updateDocument(Integer id, String category, String title, String content) {
        List<Double> vec = generateEmbedding(content);
        if (vec.isEmpty()) return;
        jdbcTemplate.update(
                "UPDATE documents SET category = ?, title = ?, content = ?, embedding = ? WHERE id = ?",
                category, title, content, toVector(vec), id);
    }

    public void deleteDocument(Integer id) {
        jdbcTemplate.update("DELETE FROM documents WHERE id = ?", id);
    }

    public List<Map<String, Object>> getAllDocuments() {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT id, category, title, content FROM documents ORDER BY id DESC");
        } catch (Exception e) {
            System.err.println("[EmbeddingService] 전체 조회 오류: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public int reembedAll() {
        List<Map<String, Object>> docs = jdbcTemplate.queryForList(
                "SELECT id, category, title, content FROM documents ORDER BY id");
        int count = 0;
        for (Map<String, Object> doc : docs) {
            try {
                updateDocument((Integer) doc.get("id"),
                        (String) doc.get("category"),
                        (String) doc.get("title"),
                        (String) doc.get("content"));
                count++;
            } catch (Exception e) {
                System.err.println("[EmbeddingService] 재임베딩 실패 id=" + doc.get("id") + ": " + e.getMessage());
            }
        }
        return count;
    }

    // ══════════════════════════════════════════════════════════════
    //  내부 구현
    // ══════════════════════════════════════════════════════════════

    /**
     * 긴 텍스트는 청크로 분리해 각각 임베딩한 뒤 평균 벡터를 반환한다.
     */
    public List<Double> generateEmbedding(String text) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += CHUNK_SIZE) {
            chunks.add(text.substring(i, Math.min(i + CHUNK_SIZE, text.length())));
        }

        List<List<Double>> embeddings = chunks.stream()
                .map(this::fetchEmbedding)
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toList());

        if (embeddings.isEmpty()) return Collections.emptyList();

        int dim = embeddings.get(0).size();
        List<Double> avg = new ArrayList<>(dim);
        for (int i = 0; i < dim; i++) {
            final int idx = i;
            double sum = embeddings.stream().mapToDouble(e -> e.get(idx)).sum();
            avg.add(sum / embeddings.size());
        }
        return avg;
    }

    /** Ollama /api/embeddings 한 번 호출 */
    private List<Double> fetchEmbedding(String text) {
        try {
            String body = mapper.writeValueAsString(Map.of("model", embeddingModel, "prompt", text));

            HttpURLConnection conn = (HttpURLConnection)
                    URI.create(ollamaApiUrl + "/embeddings").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(60_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                System.err.println("[EmbeddingService] HTTP " + conn.getResponseCode());
                return Collections.emptyList();
            }

            String resp = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<Double> vec = new ArrayList<>();
            mapper.readTree(resp).path("embedding")
                    .forEach(n -> vec.add(n.asDouble()));
            return vec;

        } catch (Exception e) {
            System.err.println("[EmbeddingService] 임베딩 오류: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private PGvector toVector(List<Double> embedding) {
        float[] f = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) f[i] = embedding.get(i).floatValue();
        return new PGvector(f);
    }
}
