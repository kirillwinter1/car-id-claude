package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import ru.car.enums.QrStatus;
import ru.car.model.Qr;
import ru.car.service.NotificationService;
import ru.car.service.QrService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrDetailsSceneTest {

    @Mock QrService qrService;
    @Mock NotificationService notificationService;

    QrDetailsScene scene;

    @BeforeEach
    void setUp() {
        scene = new QrDetailsScene(qrService, notificationService, messages());
        ReflectionTestUtils.setField(scene, "serverUrl", "https://car-id.ru");
    }

    @Test
    void keyIsQrDetails() {
        assertThat(scene.key()).isEqualTo("qr_details");
    }

    @Test
    void parentIsQrList() {
        assertThat(scene.parentKey()).isEqualTo("qr_list");
    }

    @Test
    void handle_open_rendersDetailsAsEdit() {
        UUID id = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(id);
        qr.setName("Audi Q5");
        qr.setSeqNumber(145L);
        qr.setStatus(QrStatus.ACTIVE);
        qr.setActivateDate(LocalDateTime.of(2026, 3, 21, 10, 0));
        when(qrService.findByIdOrThrowNotFound(id)).thenReturn(qr);

        SceneOutput output = scene.handle(
            new CallbackData("qr_details", "open", List.of(id.toString())),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).contains("Audi Q5").contains("145");
        assertThat(output.editInPlace()).isTrue();
        assertThat(output.inlineKeyboard()).isNotNull();
        String callbacks = output.inlineKeyboard().getKeyboard().stream()
            .flatMap(java.util.List::stream)
            .map(b -> b.getCallbackData())
            .reduce("", (a, b) -> a + "|" + b);
        assertThat(callbacks).contains("report:start:" + id);
    }

    @Test
    void handle_show_returnsPhotoWithCaption() {
        UUID id = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(id);
        qr.setName("Audi Q5");
        qr.setSeqNumber(145L);
        qr.setStatus(QrStatus.ACTIVE);
        when(qrService.findByIdOrThrowNotFound(id)).thenReturn(qr);

        SceneOutput output = scene.handle(
            new CallbackData("qr_details", "show", List.of(id.toString())),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.photo()).isNotNull();
        assertThat(output.caption()).contains("Audi Q5").contains("145");
    }

    @Test
    void handle_disable_returnsConfirmDialog() {
        UUID id = UUID.randomUUID();
        SceneOutput output = scene.handle(
            new CallbackData("qr_details", "disable", List.of(id.toString())),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).contains("Отключить метку");
        assertThat(output.editInPlace()).isTrue();
    }

    @Test
    void handle_disableConfirm_callsServiceAndRerenders() {
        UUID id = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(id);
        qr.setName("Audi Q5");
        qr.setSeqNumber(145L);
        qr.setStatus(QrStatus.DELETED);
        when(qrService.findByIdOrThrowNotFound(id)).thenReturn(qr);

        SceneOutput output = scene.handle(
            new CallbackData("qr_details", "disable_confirm", List.of(id.toString())),
            new TelegramUpdateContext(100L, 42L, null, null));

        verify(qrService).disable(id);
        assertThat(output.text()).contains("Отключена");
    }

    @Test
    void handle_invalidUuid_noop() {
        SceneOutput output = scene.handle(
            new CallbackData("qr_details", "open", List.of("not-a-uuid")),
            new TelegramUpdateContext(100L, 42L, null, null));
        assertThat(output.text()).isNull();
    }

    private static TelegramMessages messages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
