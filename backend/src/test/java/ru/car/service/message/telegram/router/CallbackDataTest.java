package ru.car.service.message.telegram.router;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackDataTest {

    @Test
    void parsesSceneAndAction() {
        Optional<CallbackData> result = CallbackData.parse("qr:list");
        assertThat(result).isPresent();
        assertThat(result.get().scene()).isEqualTo("qr");
        assertThat(result.get().action()).isEqualTo("list");
        assertThat(result.get().args()).isEmpty();
    }

    @Test
    void parsesSceneActionAndArgs() {
        Optional<CallbackData> result = CallbackData.parse("qr:pdf:550e8400-e29b-41d4-a716-446655440000");
        assertThat(result).isPresent();
        assertThat(result.get().scene()).isEqualTo("qr");
        assertThat(result.get().action()).isEqualTo("pdf");
        assertThat(result.get().args()).containsExactly("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void parsesNotificationMarkRead() {
        Optional<CallbackData> result = CallbackData.parse("notif:read:550e8400-e29b-41d4-a716-446655440000");
        assertThat(result).isPresent();
        assertThat(result.get().scene()).isEqualTo("notif");
        assertThat(result.get().action()).isEqualTo("read");
        assertThat(result.get().args()).hasSize(1);
    }

    @Test
    void returnsEmptyForBlankString() {
        assertThat(CallbackData.parse("")).isEmpty();
        assertThat(CallbackData.parse("   ")).isEmpty();
        assertThat(CallbackData.parse(null)).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoSeparator() {
        assertThat(CallbackData.parse("notacallback")).isEmpty();
    }

    @Test
    void serializeRoundTrip() {
        CallbackData data = new CallbackData("qr", "pdf", List.of("abc-123"));
        assertThat(CallbackData.parse(data.serialize())).contains(data);
    }

    @Test
    void serializeWithoutArgs() {
        CallbackData data = new CallbackData("qr", "list", List.of());
        assertThat(data.serialize()).isEqualTo("qr:list");
    }
}
