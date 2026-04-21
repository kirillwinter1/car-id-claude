package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import ru.car.enums.BatchTemplates;
import ru.car.model.Batch;
import ru.car.model.Qr;
import ru.car.repository.BatchRepository;
import ru.car.repository.QrRepository;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.transport.TelegramTransport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrListSceneTest {

    @Mock QrRepository qrRepository;
    @Mock BatchRepository batchRepository;
    @Mock TelegramTransport transport;

    private QrListScene scene;

    private final Executor syncExecutor = Runnable::run;

    @BeforeEach
    void setUp() {
        TelegramMessages msgs = messages();
        scene = new QrListScene(qrRepository, batchRepository, transport, syncExecutor, msgs, new HomeMenuScene(msgs));
    }

    @Test
    void keyIsQr() {
        assertThat(scene.key()).isEqualTo("qr");
    }

    @Test
    void canHandleText_onlyForQrsButton() {
        assertThat(scene.canHandleText("QR-коды")).isTrue();
        assertThat(scene.canHandleText("Временный QR")).isFalse();
    }

    @Test
    void render_emptyList_returnsEmptyMessage() {
        when(qrRepository.findByUserId(1L)).thenReturn(List.of());
        SceneOutput output = scene.render(new TelegramUpdateContext(10L, 1L, null, null));

        assertThat(output.text()).contains("На текущий момент у Вас нет QR");
        assertThat(output.inlineKeyboard()).isNull();
        assertThat(output.replyKeyboard()).isNotNull();  // главное меню прилагается
    }

    @Test
    void render_withQrs_returnsListWithInlineButtons() {
        UUID id = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(id);
        qr.setName("Audi");
        qr.setSeqNumber(123L);
        when(qrRepository.findByUserId(1L)).thenReturn(List.of(qr));

        SceneOutput output = scene.render(new TelegramUpdateContext(10L, 1L, null, null));

        assertThat(output.text()).isEqualTo("QR-коды:");
        assertThat(output.inlineKeyboard()).isNotNull();
        assertThat(output.inlineKeyboard().getKeyboard()).hasSize(1);
        assertThat(output.inlineKeyboard().getKeyboard().get(0).get(0).getText()).isEqualTo("Audi 123");
        assertThat(output.inlineKeyboard().getKeyboard().get(0).get(0).getCallbackData()).isEqualTo("qr:pdf:" + id);
    }

    @Test
    void handle_list_returnsEditMarkupWithQrs() {
        UUID id = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(id);
        qr.setName("Audi");
        qr.setSeqNumber(123L);
        when(qrRepository.findByUserId(1L)).thenReturn(List.of(qr));

        SceneOutput output = scene.handle(
                new CallbackData("qr", "list", List.of()),
                new TelegramUpdateContext(10L, 1L, null, null));

        assertThat(output.editInPlace()).isTrue();
        assertThat(output.inlineKeyboard()).isNotNull();
    }

    @Test
    void handle_pdf_sendsDocument() {
        UUID id = UUID.randomUUID();
        Batch batch = new Batch();
        batch.setTemplate(BatchTemplates.PT_WHITE_1);
        when(batchRepository.findByQrId(id)).thenReturn(Optional.of(batch));

        SceneOutput output = scene.handle(
                new CallbackData("qr", "pdf", List.of(id.toString())),
                new TelegramUpdateContext(10L, 1L, null, null));

        assertThat(output.text()).isNull();
        verify(transport).sendDocument(any(SendDocument.class));
    }

    @Test
    void handle_pdf_invalidUuid_noop() {
        SceneOutput output = scene.handle(
                new CallbackData("qr", "pdf", List.of("junk")),
                new TelegramUpdateContext(10L, 1L, null, null));
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
