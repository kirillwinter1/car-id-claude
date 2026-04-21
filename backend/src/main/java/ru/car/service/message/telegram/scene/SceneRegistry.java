package ru.car.service.message.telegram.scene;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SceneRegistry {

    private final Map<String, TelegramScene> scenesByKey;
    private final List<TelegramScene> scenes;

    public SceneRegistry(List<TelegramScene> scenes) {
        this.scenes = scenes;
        this.scenesByKey = scenes.stream()
                .collect(Collectors.toMap(TelegramScene::key, Function.identity()));
    }

    public Optional<TelegramScene> findByKey(String key) {
        return Optional.ofNullable(scenesByKey.get(key));
    }

    public Optional<TelegramScene> findByText(String text) {
        if (text == null) {
            return Optional.empty();
        }
        return scenes.stream()
                .filter(scene -> scene.canHandleText(text))
                .findFirst();
    }
}
