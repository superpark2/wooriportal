package com.mrpark.dev.wooriportal.hrd.ssv;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.Deflater;

/**
 * {@link SsvData} 를 Nexacro SSV(바이너리)로 인코딩한다. {@link SsvDecoder} 의 역연산.
 *
 * <p>디코드한 캡처 요청의 변수/셀 값만 바꿔 다시 인코딩하면 원본과 동일한 구조의
 * HRD 요청 본문을 만들 수 있다(라운드트립 바이트 일치 검증됨).</p>
 *
 * <h3>인코딩 규칙</h3>
 * <ul>
 *   <li>변수블록 blen = 2(count) + Σ엔트리바이트, 변수 타입 = 0x0015</li>
 *   <li>데이터셋 blen = nameField + 컬럼블록 (행 데이터 미포함)</li>
 *   <li>rowByteLen = 4 + Σ셀바이트, rowState/인코딩셀수는 원본 보존</li>
 *   <li>셀 타입: 컬럼타입 1→0x15(문자), 4→0x28(숫자)</li>
 *   <li>각 데이터셋 끝에 트레일러 00 00 00 00</li>
 * </ul>
 */
public final class SsvEncoder {

    private static final int VAR_TYPE = 0x0015;
    private static final int CONST = 0x1388;

    private SsvEncoder() {
    }

    /** HTTP 전송용 본문: FF AD + zlib(deflate). */
    public static byte[] encode(SsvData data) {
        return frame(encodeInflated(data));
    }

    /** 압축 전 SSV 바이너리(라운드트립 검증/디버그용). */
    public static byte[] encodeInflated(SsvData data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);

        // ── 변수블록 ──
        ByteArrayOutputStream entries = new ByteArrayOutputStream();
        for (Map.Entry<String, String> e : data.getVariables().entrySet()) {
            writeStr(entries, e.getKey());
            writeU16(entries, VAR_TYPE);
            writeStr(entries, e.getValue());
        }
        out.write(0xFE);
        out.write(0x10);
        writeU16(out, CONST);
        writeU16(out, 2 + entries.size());
        writeU16(out, data.getVariables().size());
        writeAll(out, entries);

        // ── 데이터셋들 ──
        for (SsvDataset ds : data.getDatasets().values()) {
            ByteArrayOutputStream colblk = new ByteArrayOutputStream();
            colblk.write(0xFE);
            colblk.write(0x10);
            writeU16(colblk, CONST);
            writeU16(colblk, ds.getColBlockX());
            writeU16(colblk, ds.getColBlockY());
            writeU16(colblk, ds.getColumnCount());
            for (SsvColumn c : ds.getColumnDefs()) {
                writeStr(colblk, c.name());
                writeU16(colblk, c.type());
                writeU16(colblk, c.size());
                writeU16(colblk, c.flag());
            }

            byte[] nameBytes = ds.getName().getBytes(StandardCharsets.UTF_8);
            int nameField = 2 + nameBytes.length;

            out.write(0xFE);
            out.write(0x01);
            writeU16(out, CONST);
            writeU16(out, nameField + colblk.size());
            writeU16(out, nameBytes.length);
            out.writeBytes(nameBytes);
            writeAll(out, colblk);

            for (int ri = 0; ri < ds.getRowCount(); ri++) {
                SsvDataset.Row row = ds.rowAt(ri);
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                for (int ci = 0; ci < row.encodedCount; ci++) {
                    String v = row.values[ci];
                    if (v == null) {
                        writeU16(body, 0);
                    } else {
                        writeU16(body, cellType(ds.getColumnDefs().get(ci).type()));
                        byte[] vb = v.getBytes(StandardCharsets.UTF_8);
                        writeU16(body, vb.length);
                        body.writeBytes(vb);
                    }
                }
                writeU16(out, 4 + body.size());
                writeU16(out, row.rowState);
                writeU16(out, row.encodedCount);
                writeAll(out, body);
            }

            out.write(0x00);
            out.write(0x00);
            out.write(0x00);
            out.write(0x00);
        }

        return out.toByteArray();
    }

    private static int cellType(int columnType) {
        return switch (columnType) {
            case 4 -> 0x28;   // 숫자
            default -> 0x15;  // 문자
        };
    }

    private static byte[] frame(byte[] inflated) {
        Deflater deflater = new Deflater();
        deflater.setInput(inflated);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(inflated.length / 2 + 16);
        out.write(0xFF);
        out.write(0xAD);
        byte[] chunk = new byte[8192];
        while (!deflater.finished()) {
            int n = deflater.deflate(chunk);
            out.write(chunk, 0, n);
        }
        deflater.end();
        return out.toByteArray();
    }

    private static void writeU16(ByteArrayOutputStream o, int v) {
        o.write((v >>> 8) & 0xFF);
        o.write(v & 0xFF);
    }

    private static void writeStr(ByteArrayOutputStream o, String s) {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        writeU16(o, b.length);
        o.writeBytes(b);
    }

    private static void writeAll(ByteArrayOutputStream o, ByteArrayOutputStream src) {
        o.writeBytes(src.toByteArray());
    }
}
