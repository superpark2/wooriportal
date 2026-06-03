package com.mrpark.dev.wooriportal.hrd.ssv;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * HRD-Net(고용노동부 직업훈련) 행정시스템이 쓰는 Nexacro/XPLATFORM SSV(바이너리) 응답 디코더.
 *
 * <h3>프레임</h3>
 * <pre>
 *   [0xFF 0xAD] + zlib(deflate) 압축 페이로드      ← 압축본
 *   압축 해제하면 SSV 바이너리(문자열 charset = UTF-8)
 * </pre>
 *
 * <h3>SSV 바이너리 문법 (big-endian u8/u16)</h3>
 * <pre>
 *   변수목록   : FE 10 1388 [u16 byteLen][u16 count]
 *                 count x { [u16 nameLen] name [u16 type=0x0015][u16 valLen] value }
 *   데이터셋   : FE 01 1388 [u16 byteLen][u16 nameLen] name
 *                 컬럼블록 : FE 10 1388 [u16][u16][u16 colCount]
 *                            colCount x { [u16 nameLen] name [u16 type][u16 size][u16 flag] }
 *                 행 반복(트레일러 전까지) :
 *                            [u16 rowByteLen][u16 rowState][u16 colCount]
 *                            colCount x cell
 *                 트레일러 : 00 00 00 00   (뒤에 0xFE 또는 EOF)
 *   cell      : [u16 type] (0x0000 → null) | [u16 type!=0][u16 len] valueBytes
 * </pre>
 *
 * 문자열에는 0xFE/0xFF 바이트가 등장하지 않으므로(UTF-8 불변식) 길이 기반 파싱이 안전하다.
 */
public final class SsvDecoder {

    private static final int MARKER_FF = 0xFF;
    private static final int MARKER_AD = 0xAD;
    private static final int REC = 0xFE;
    private static final int REC_VARLIST = 0x10;
    private static final int REC_DATASET = 0x01;

    private final byte[] buf;
    private int pos;

    private SsvDecoder(byte[] inflated) {
        this.buf = inflated;
        this.pos = 0;
    }

    /** 원본 HTTP 바디(FF AD + zlib)를 받아 디코딩한다. */
    public static SsvData decode(byte[] httpBody) {
        return new SsvDecoder(inflate(httpBody)).parse();
    }

    /** 이미 압축 해제된 SSV 바이너리를 디코딩한다(테스트/디버그용). */
    public static SsvData decodeInflated(byte[] inflated) {
        return new SsvDecoder(inflated).parse();
    }

    static byte[] inflate(byte[] httpBody) {
        int offset = 0;
        if (httpBody.length >= 2
                && (httpBody[0] & 0xFF) == MARKER_FF
                && (httpBody[1] & 0xFF) == MARKER_AD) {
            offset = 2;
        }
        Inflater inflater = new Inflater();
        inflater.setInput(httpBody, offset, httpBody.length - offset);
        ByteArrayOutputStream out = new ByteArrayOutputStream(httpBody.length * 3);
        byte[] chunk = new byte[8192];
        try {
            while (!inflater.finished()) {
                int n = inflater.inflate(chunk);
                if (n == 0) {
                    if (inflater.finished() || inflater.needsDictionary()) {
                        break;
                    }
                    if (inflater.needsInput()) {
                        break; // 더 줄 입력이 없음
                    }
                }
                out.write(chunk, 0, n);
            }
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("SSV zlib 해제 실패", e);
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }

    private SsvData parse() {
        SsvData data = new SsvData();
        boolean first = true;
        while (pos < buf.length) {
            if (u8peek() != REC) {
                break;
            }
            u8();                 // FE
            int rtype = u8();
            u16();                // 0x1388 const
            if (first && rtype == REC_VARLIST) {
                readVariables(data);
                first = false;
                continue;
            }
            first = false;
            if (rtype != REC_DATASET) {
                break;
            }
            readDataset(data);
        }
        return data;
    }

    private void readVariables(SsvData data) {
        u16();                    // byteLen
        int count = u16();
        for (int i = 0; i < count; i++) {
            String name = str(u16());
            u16();                // type (0x0015)
            String value = str(u16());
            data.putVariable(name, value);
        }
    }

    private void readDataset(SsvData data) {
        u16();                    // byteLen (= nameField + 컬럼블록, 인코딩 시 재계산)
        String name = str(u16());

        // 컬럼 블록: FE 10 1388
        u8();                     // FE
        u8();                     // 10
        u16();                    // 1388
        int x = u16();
        int y = u16();
        int colCount = u16();
        List<SsvColumn> columns = new ArrayList<>(colCount);
        for (int c = 0; c < colCount; c++) {
            String colName = str(u16());
            int type = u16();
            int size = u16();
            int flag = u16();
            columns.add(new SsvColumn(colName, type, size, flag));
        }

        SsvDataset dataset = new SsvDataset(name, columns, x, y);
        while (!atTrailer()) {
            u16();                // rowByteLen (인코딩 시 재계산)
            int rowState = u16();
            int rc = u16();       // 인코딩된 셀 수(뒤쪽 null 은 생략될 수 있음)
            String[] row = new String[colCount];
            for (int c = 0; c < rc; c++) {
                int type = u16();
                if (type == 0) {
                    row[c] = null;
                } else {
                    row[c] = str(u16());
                }
            }
            dataset.addRow(row, rowState, rc);
        }
        pos += 4;                 // 트레일러 00 00 00 00
        data.putDataset(dataset);
    }

    /** 현재 위치가 데이터셋 종료 트레일러(00 00 00 00 + FE/EOF)인지. */
    private boolean atTrailer() {
        if (pos + 4 > buf.length) {
            return true;          // 입력 끝 — 더 읽을 행 없음
        }
        if (buf[pos] != 0 || buf[pos + 1] != 0 || buf[pos + 2] != 0 || buf[pos + 3] != 0) {
            return false;
        }
        int after = pos + 4;
        return after >= buf.length || (buf[after] & 0xFF) == REC;
    }

    // ---- 저수준 리더 ----

    private int u8() {
        return buf[pos++] & 0xFF;
    }

    private int u8peek() {
        return buf[pos] & 0xFF;
    }

    private int u16() {
        int v = ((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF);
        pos += 2;
        return v;
    }

    /** 길이 prefix 가 이미 읽힌 상태에서 len 바이트를 UTF-8 문자열로 읽는다. */
    private String str(int len) {
        String s = new String(buf, pos, len, StandardCharsets.UTF_8);
        pos += len;
        return s;
    }
}
