package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import ru.car.enums.QrStatus;
import ru.car.model.Qr;
import ru.car.repository.QrRepository;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrListSceneTest {

    @Mock QrRepository qrRepository;

    QrListScene scene;

    @BeforeEach
    void setUp() {
        scene = new QrListScene(qrRepository, messages());
    }

    @Test
    void keyIsQrList() {
        assertThat(scene.key()).isEqualTo("qr_list");
    }

    @Test
    void parentIsHome() {
        assertThat(scene.parentKey()).isEqualTo("home");
    }

    @Test
    void canHandleText_qrCodes() {
        assertThat(scene.canHandleText("QR-коды")).isTrue();
        assertThat(scene.canHandleText("random")).isFalse();
    }

    @Test
    void render_empty_returnsEmptyCardWithOzonButton() {
        when(qrRepository.findByUserId(42L)).thenReturn(List.of());

        SceneOutput output = scene.render(new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).contains("Мои метки");
        assertThat(output.parseMode()).isEqualTo("HTML");
        assertThat(output.inlineKeyboard()).isNotNull();
    }

    @Test
    void render_withActiveQr_buildsListAndButtons() {
        UUID id1 = UUID.randomUUID();
        Qr active = new Qr();
        active.setId(id1);
        active.setName("Audi Q5");
        active.setSeqNumber(145L);
        active.setStatus(QrStatus.ACTIVE);
        active.setActivateDate(LocalDateTime.of(2026, 3, 21, 10, 0));
        active.setCreatedDate(LocalDateTime.of(2026, 3, 21, 10, 0));

        when(qrRepository.findByUserId(42L)).thenReturn(List.of(active));

        SceneOutput output = scene.render(new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).contains("Audi Q5").contains("145");
        // >= 2 rows: 1 qr + back
        assertThat(output.inlineKeyboard().getKeyboard()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void handle_open_returnsEditInPlace() {
        when(qrRepository.findByUserId(42L)).thenReturn(List.of());

        SceneOutput output = scene.handle(
            new CallbackData("qr_list", "open", List.of()),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.editInPlace()).isTrue();
    }

    private static TelegramMessages messages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
