package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import ru.car.dto.QrDto;
import ru.car.exception.NotFoundException;
import ru.car.service.QrService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemporaryQrSceneTest {

    @Mock QrService qrService;

    private TemporaryQrScene scene;

    @BeforeEach
    void setUp() {
        TelegramMessages msgs = messages();
        scene = new TemporaryQrScene(qrService, msgs, new HomeMenuScene(msgs));
        ReflectionTestUtils.setField(scene, "url", "https://car-id.ru");
    }

    @Test
    void keyIsTempQr() {
        assertThat(scene.key()).isEqualTo("temp_qr");
    }

    @Test
    void canHandleTextReturnsTrueForButton() {
        assertThat(scene.canHandleText("Временный QR")).isTrue();
        assertThat(scene.canHandleText("qr-коды")).isFalse();
        assertThat(scene.canHandleText(null)).isFalse();
    }

    @Test
    void renderSuccess_buildsCreatedMessage() {
        UUID qrId = UUID.randomUUID();
        when(qrService.createTemporaryQr(1L)).thenReturn(QrDto.builder().qrId(qrId).build());

        SceneOutput output = scene.render(new TelegramUpdateContext(10L, 1L, null, null));

        assertThat(output.text())
                .contains(qrId.toString())
                .contains("https://car-id.ru/qr/" + qrId);
        assertThat(output.replyKeyboard()).isNotNull();  // главное меню остаётся внизу
    }

    @Test
    void renderAppException_returnsExceptionMessage() {
        when(qrService.createTemporaryQr(1L)).thenThrow(new NotFoundException("Превышен лимит"));

        SceneOutput output = scene.render(new TelegramUpdateContext(10L, 1L, null, null));
        assertThat(output.text()).isEqualTo("Превышен лимит");
    }

    @Test
    void renderUnexpectedException_returnsErrorText() {
        when(qrService.createTemporaryQr(1L)).thenThrow(new RuntimeException("boom"));

        SceneOutput output = scene.render(new TelegramUpdateContext(10L, 1L, null, null));
        assertThat(output.text()).isEqualTo("внутренняя ошибка при создании временного кода");
    }

    private static TelegramMessages messages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
