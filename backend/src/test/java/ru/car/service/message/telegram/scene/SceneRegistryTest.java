package ru.car.service.message.telegram.scene;

import org.junit.jupiter.api.Test;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SceneRegistryTest {

    private final TelegramScene sceneA = new NamedScene("a", "кнопка А");
    private final TelegramScene sceneB = new NamedScene("b", "кнопка Б");
    private final SceneRegistry registry = new SceneRegistry(List.of(sceneA, sceneB));

    @Test
    void findsByKey() {
        assertThat(registry.findByKey("a")).contains(sceneA);
        assertThat(registry.findByKey("b")).contains(sceneB);
        assertThat(registry.findByKey("missing")).isEmpty();
    }

    @Test
    void findsByTextTrigger() {
        assertThat(registry.findByText("кнопка А")).contains(sceneA);
        assertThat(registry.findByText("кнопка Б")).contains(sceneB);
        assertThat(registry.findByText("nothing")).isEmpty();
    }

    private static final class NamedScene implements TelegramScene {
        private final String key;
        private final String textTrigger;

        NamedScene(String key, String textTrigger) {
            this.key = key;
            this.textTrigger = textTrigger;
        }

        @Override public String key() { return key; }
        @Override public boolean canHandleText(String text) { return textTrigger.equals(text); }
        @Override public SceneOutput render(TelegramUpdateContext ctx) { return SceneOutput.noop(); }
        @Override public SceneOutput handle(CallbackData d, TelegramUpdateContext ctx) { return SceneOutput.noop(); }
    }
}
