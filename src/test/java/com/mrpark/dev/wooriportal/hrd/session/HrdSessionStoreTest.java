package com.mrpark.dev.wooriportal.hrd.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HrdSessionStoreTest {

    @Test
    void updateDetectsChange() {
        HrdSessionStore store = new HrdSessionStore();
        assertThat(store.isPresent()).isFalse();

        assertThat(store.update("JS1", "W1")).as("최초 주입").isTrue();
        assertThat(store.isPresent()).isTrue();

        assertThat(store.update("JS1", "W1")).as("동일 값 재주입").isFalse();
        assertThat(store.update("JS2", "W1")).as("세션 변경").isTrue();
    }

    @Test
    void ignoresBlankSession() {
        HrdSessionStore store = new HrdSessionStore();
        assertThat(store.update(null, "W1")).isFalse();
        assertThat(store.update("  ", "W1")).isFalse();
        assertThat(store.isPresent()).isFalse();
    }

    @Test
    void buildsCookieHeader() {
        HrdSessionStore store = new HrdSessionStore();
        store.update("Qxrz!-123!-456", "R8QorMU679h");

        String cookie = store.current().orElseThrow().toCookieHeader();
        assertThat(cookie)
                .contains("WMONID=R8QorMU679h")
                .contains("JSESSIONID=Qxrz!-123!-456")
                .startsWith("gv_ssoFlag=;");
    }
}
