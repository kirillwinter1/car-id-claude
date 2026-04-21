package ru.car.service.message.telegram.scene.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.car.dto.QrDto;
import ru.car.exception.AppException;
import ru.car.service.QrService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;

@Slf4j
@Component
public class TemporaryQrScene implements TelegramScene {

    public static final String KEY = "temp_qr";

    private final QrService qrService;
    private final TelegramMessages messages;
    private final HomeMenuScene homeMenuScene;

    @Value("${swagger.server.url}")
    private String url;

    public TemporaryQrScene(QrService qrService, TelegramMessages messages, HomeMenuScene homeMenuScene) {
        this.qrService = qrService;
        this.messages = messages;
        this.homeMenuScene = homeMenuScene;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public boolean canHandleText(String text) {
        return messages.get("tg.home.btn.temp_qr").equals(text);
    }

    @Override
    public SceneOutput render(TelegramUpdateContext ctx) {
        try {
            QrDto qr = qrService.createTemporaryQr(ctx.userId());
            String text = messages.get("tg.temp_qr.created", qr.getQrId(), url, qr.getQrId());
            return SceneOutput.send(text, homeMenuScene.mainKeyboard());
        } catch (AppException e) {
            return SceneOutput.send(e.getMessage(), homeMenuScene.mainKeyboard());
        } catch (Exception e) {
            log.error("внутренняя ошибка при создании временного кода", e);
            return SceneOutput.send(messages.get("tg.temp_qr.error"), homeMenuScene.mainKeyboard());
        }
    }
}
