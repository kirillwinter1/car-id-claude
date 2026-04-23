package ru.car.service.message.telegram.scene.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.car.dto.QrDto;
import ru.car.exception.AppException;
import ru.car.service.QrService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.util.QrUtils;

import java.io.IOException;

@Slf4j
@Component
public class TemporaryQrScene implements TelegramScene {

    public static final String KEY = "temp_qr";
    public static final String ACTION_CREATE = "create";

    private final QrService qrService;
    private final TelegramMessages messages;

    @Value("${swagger.server.url}")
    private String url;

    public TemporaryQrScene(QrService qrService, TelegramMessages messages) {
        this.qrService = qrService;
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public boolean canHandleText(String text) {
        return "Временный QR".equals(text);
    }

    @Override
    public SceneOutput render(TelegramUpdateContext ctx) {
        return create(ctx.userId());
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        if (ACTION_CREATE.equals(data.action())) {
            return create(ctx.userId());
        }
        return SceneOutput.noop();
    }

    private SceneOutput create(Long userId) {
        try {
            QrDto qr = qrService.createTemporaryQr(userId);
            String qrUrl = url + "/qr/" + qr.getQrId();
            String caption = messages.get("tg.temp_qr.caption", qrUrl);
            Resource pngResource = QrUtils.generateQRCodeImage(qrUrl);
            InputFile input = new InputFile(pngResource.getInputStream(), "temp-qr-" + qr.getQrId() + ".png");
            return SceneOutput.photo(input, caption);
        } catch (AppException e) {
            return SceneOutput.sendHtml(e.getMessage(), null);
        } catch (IOException e) {
            log.error("Failed to build temp QR PNG", e);
            return SceneOutput.sendHtml(messages.get("tg.temp_qr.error"), null);
        } catch (Exception e) {
            log.error("внутренняя ошибка при создании временного кода", e);
            return SceneOutput.sendHtml(messages.get("tg.temp_qr.error"), null);
        }
    }
}
