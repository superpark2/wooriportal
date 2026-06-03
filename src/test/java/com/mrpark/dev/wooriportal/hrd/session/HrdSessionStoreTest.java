package com.mrpark.dev.wooriportal.hrd.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.mrpark.dev.wooriportal.hrd.session.HrdSessionStore.Result;
import org.junit.jupiter.api.Test;

class HrdSessionStoreTest {

    @Test
    void firstConnectAccepted() {
        HrdSessionStore store = new HrdSessionStore();
        assertThat(store.isPresent()).isFalse();
        assertThat(store.tryUpdate("JS1", "W1", "alice")).isEqualTo(Result.ACCEPTED);
        assertThat(store.isPresent()).isTrue();
        assertThat(store.current().orElseThrow().getSource()).isEqualTo("alice");
    }

    @Test
    void sameOwnerCanRefresh() {
        HrdSessionStore store = new HrdSessionStore();
        store.tryUpdate("JS1", "W1", "alice");
        assertThat(store.tryUpdate("JS2", "W1", "alice")).isEqualTo(Result.ACCEPTED);
        assertThat(store.current().orElseThrow().getJsessionId()).isEqualTo("JS2");
    }

    @Test
    void otherOwnerRejectedWhenOccupied() {
        HrdSessionStore store = new HrdSessionStore();
        store.tryUpdate("JS1", "W1", "alice");
        assertThat(store.tryUpdate("JSX", "W1", "bob")).isEqualTo(Result.REJECTED);
        assertThat(store.current().orElseThrow().getSource()).isEqualTo("alice");
    }

    @Test
    void takeoverWindowLetsOtherOwnerIn() {
        HrdSessionStore store = new HrdSessionStore();
        store.tryUpdate("JS1", "W1", "alice");

        store.openTakeover(90);               // 웹에서 전환 확인
        assertThat(store.isPresent()).isFalse(); // 기존 연동 해제됨
        assertThat(store.tryUpdate("JS2", "W1", "bob")).isEqualTo(Result.ACCEPTED);
        assertThat(store.current().orElseThrow().getSource()).isEqualTo("bob");
    }

    @Test
    void blankSessionRejected() {
        HrdSessionStore store = new HrdSessionStore();
        assertThat(store.tryUpdate(null, "W1", "alice")).isEqualTo(Result.REJECTED);
        assertThat(store.tryUpdate("  ", "W1", "alice")).isEqualTo(Result.REJECTED);
        assertThat(store.isPresent()).isFalse();
    }

    @Test
    void disconnectClears() {
        HrdSessionStore store = new HrdSessionStore();
        store.tryUpdate("JS1", "W1", "alice");
        store.disconnect();
        assertThat(store.isPresent()).isFalse();
    }
}
