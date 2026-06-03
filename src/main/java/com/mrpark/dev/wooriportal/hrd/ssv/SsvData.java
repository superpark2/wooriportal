package com.mrpark.dev.wooriportal.hrd.ssv;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 한 번의 SSV 트랜잭션(요청 또는 응답) 전체.
 *
 * <p>최상단 변수목록({@link #getVariables()})과 0개 이상의 {@link SsvDataset} 을 담는다.
 * 변수는 본질적으로 key/value 라 작은 Map 으로 보관한다.</p>
 */
public class SsvData {

    private final Map<String, String> variables = new LinkedHashMap<>();
    private final Map<String, SsvDataset> datasets = new LinkedHashMap<>();

    public Map<String, String> getVariables() {
        return variables;
    }

    public String getVariable(String name) {
        return variables.get(name);
    }

    public Map<String, SsvDataset> getDatasets() {
        return datasets;
    }

    public SsvDataset getDataset(String name) {
        return datasets.get(name);
    }

    void putVariable(String name, String value) {
        variables.put(name, value);
    }

    /** 변수 값을 수정한다(요청 빌드용). 없으면 새로 추가. */
    public void setVariable(String name, String value) {
        variables.put(name, value);
    }

    void putDataset(SsvDataset dataset) {
        datasets.put(dataset.getName(), dataset);
    }
}
