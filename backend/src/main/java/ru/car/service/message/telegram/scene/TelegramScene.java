package ru.car.service.message.telegram.scene;

import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;

public interface TelegramScene {

    /**
     * Ключ сцены, совпадает с `scene` в callback_data.
     */
    String key();

    /**
     * Возвращает true, если сцена должна обработать входящий текст как триггер
     * (например, текст кнопки reply-клавиатуры).
     */
    default boolean canHandleText(String text) {
        return false;
    }

    /**
     * Первичный рендер сцены (например, по текстовой команде).
     */
    default SceneOutput render(TelegramUpdateContext ctx) {
        return SceneOutput.noop();
    }

    /**
     * Обработка нажатия inline-кнопки сцены.
     */
    default SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        return SceneOutput.noop();
    }

    /**
     * Ключ родительской сцены — роутер использует при `<scene>:back` callback.
     * Default "home" — для большинства сцен back возвращает на главный экран.
     */
    default String parentKey() {
        return "home";
    }
}
