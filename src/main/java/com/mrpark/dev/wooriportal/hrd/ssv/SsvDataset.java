package com.mrpark.dev.wooriportal.hrd.ssv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nexacro(XPLATFORM) SSV 의 단일 Dataset.
 *
 * <p>컬럼이 응답마다 동적이라 코덱 레벨에서는 제네릭 컨테이너로 보관하고,
 * 도메인에서는 {@code getString(row, col)} 로 꺼내 전용 DTO 로 매핑한다.</p>
 *
 * <p>바이트 단위 재인코딩(요청 빌드)을 위해 컬럼 메타와 행별 인코딩 정보
 * (rowState, 인코딩된 셀 수)를 함께 보존한다.</p>
 */
public class SsvDataset {

    /** 컬럼블록 헤더 상수(원본 보존용, 통상 x=2 y=0). */
    private final int colBlockX;
    private final int colBlockY;

    private final String name;
    private final List<SsvColumn> columns;
    private final Map<String, Integer> columnIndex;
    private final List<Row> rows = new ArrayList<>();

    public SsvDataset(String name, List<SsvColumn> columns) {
        this(name, columns, 2, 0);
    }

    public SsvDataset(String name, List<SsvColumn> columns, int colBlockX, int colBlockY) {
        this.name = name;
        this.columns = List.copyOf(columns);
        this.colBlockX = colBlockX;
        this.colBlockY = colBlockY;
        this.columnIndex = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            columnIndex.put(columns.get(i).name(), i);
        }
    }

    /** 디코더가 사용하는 행 추가. values 길이는 컬럼 수와 같아야 한다. */
    void addRow(String[] values, int rowState, int encodedCount) {
        rows.add(new Row(values, rowState, encodedCount));
    }

    public String getName() {
        return name;
    }

    /** 컬럼명 목록. */
    public List<String> getColumns() {
        List<String> names = new ArrayList<>(columns.size());
        for (SsvColumn c : columns) {
            names.add(c.name());
        }
        return names;
    }

    List<SsvColumn> getColumnDefs() {
        return columns;
    }

    int getColBlockX() {
        return colBlockX;
    }

    int getColBlockY() {
        return colBlockY;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public int getRowCount() {
        return rows.size();
    }

    public boolean hasColumn(String column) {
        return columnIndex.containsKey(column);
    }

    /** 해당 컬럼이 존재하지 않으면 {@code null}. */
    public String getString(int rowIdx, String column) {
        Integer idx = columnIndex.get(column);
        if (idx == null) {
            return null;
        }
        return rows.get(rowIdx).values[idx];
    }

    /**
     * 셀 값을 수정한다(요청 빌드용). 컬럼이 없거나 행 범위를 벗어나면 무시.
     *
     * <p>값을 null→비null 로 바꾸면 인코딩 셀 수가 늘 수 있어, 변경 컬럼 인덱스까지
     * 인코딩 범위를 확장한다.</p>
     */
    public void setString(int rowIdx, String column, String value) {
        Integer idx = columnIndex.get(column);
        if (idx == null || rowIdx < 0 || rowIdx >= rows.size()) {
            return;
        }
        Row row = rows.get(rowIdx);
        row.values[idx] = value;
        if (value != null && idx + 1 > row.encodedCount) {
            row.encodedCount = idx + 1;
        }
    }

    Row rowAt(int idx) {
        return rows.get(idx);
    }

    /** 행: 값 배열 + 인코딩 메타. */
    static final class Row {
        final String[] values;
        final int rowState;
        int encodedCount;

        Row(String[] values, int rowState, int encodedCount) {
            this.values = values;
            this.rowState = rowState;
            this.encodedCount = encodedCount;
        }
    }
}
