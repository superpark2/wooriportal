package com.mrpark.dev.wooriportal.hrd;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assumptions;

/**
 * HRD 캡처 테스트 픽스처 로더.
 *
 * <p>캡처(`src/test/resources/hrd/*.hex`)에는 실제 공인인증서·이름·주민번호가 들어 있어
 * git 에서 제외(.gitignore)된다. 로컬에 파일이 있으면 테스트가 돌고, 없으면(클론/CI)
 * {@link Assumptions} 로 우아하게 스킵한다.</p>
 */
public final class HrdCaptures {

    private HrdCaptures() {
    }

    /** hex 캡처를 바이트로. 리소스가 없으면 테스트를 스킵한다. */
    public static byte[] load(String resource) {
        try (InputStream in = HrdCaptures.class.getResourceAsStream(resource)) {
            Assumptions.assumeTrue(in != null, "PII 캡처 미존재(로컬 전용) — 스킵: " + resource);
            String[] tokens = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim().split("\\s+");
            byte[] out = new byte[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                out[i] = (byte) Integer.parseInt(tokens[i], 16);
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
