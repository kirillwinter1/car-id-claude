package ru.car.service.message.telegram.scene.state;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SceneStateRegistryTest {

    private final SceneStateRegistry registry = new SceneStateRegistry();

    @Test
    void peekReturnsEmptyWhenNoState() {
        assertThat(registry.peek(100L)).isEmpty();
    }

    @Test
    void putThenPeekReturnsState() {
        registry.put(100L, "report", "text", List.of("qr", "42"));
        Optional<SceneStateRegistry.PendingText> state = registry.peek(100L);
        assertThat(state).isPresent();
        assertThat(state.get().scene()).isEqualTo("report");
        assertThat(state.get().action()).isEqualTo("text");
        assertThat(state.get().args()).containsExactly("qr", "42");
        assertThat(state.get().draftText()).isNull();
    }

    @Test
    void popRemovesState() {
        registry.put(100L, "support", "text", List.of());
        assertThat(registry.pop(100L)).isPresent();
        assertThat(registry.peek(100L)).isEmpty();
    }

    @Test
    void updateDraftStoresUserText() {
        registry.put(100L, "report", "text", List.of("qr", "42"));
        registry.updateDraft(100L, "it broke");
        assertThat(registry.peek(100L).orElseThrow().draftText()).isEqualTo("it broke");
    }

    @Test
    void updateDraftDoesNothingIfNoState() {
        registry.updateDraft(100L, "ignored");
        assertThat(registry.peek(100L)).isEmpty();
    }

    @Test
    void expiredStateIsEvicted() throws InterruptedException {
        registry.putWithTtl(100L, "support", "text", List.of(), Duration.ofMillis(50));
        Thread.sleep(80);
        assertThat(registry.peek(100L)).isEmpty();
    }

    @Test
    void clearRemovesState() {
        registry.put(100L, "report", "text", List.of());
        registry.clear(100L);
        assertThat(registry.peek(100L)).isEmpty();
    }
}
