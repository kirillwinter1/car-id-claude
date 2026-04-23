package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import ru.car.dto.NotificationDto;
import ru.car.dto.ReasonDictionaryDto;
import ru.car.enums.QrStatus;
import ru.car.model.Qr;
import ru.car.model.User;
import ru.car.repository.QrRepository;
import ru.car.service.NotificationFacade;
import ru.car.service.ReasonDictionaryService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.state.SceneStateRegistry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportEventSceneTest {

    @Mock NotificationFacade notificationFacade;
    @Mock QrRepository qrRepository;
    @Mock ReasonDictionaryService reasonDictionaryService;
    @Mock SceneStateRegistry stateRegistry;

    ReportEventScene scene;

    @BeforeEach
    void setUp() {
        scene = new ReportEventScene(notificationFacade, qrRepository, reasonDictionaryService, stateRegistry, messages());
    }

    @Test
    void keyIsReport() {
        assertThat(scene.key()).isEqualTo("report");
    }

    @Test
    void start_withoutQrId_rendersQrList() {
        Qr q = new Qr();
        q.setId(UUID.randomUUID());
        q.setName("Audi");
        q.setSeqNumber(145L);
        q.setStatus(QrStatus.ACTIVE);
        when(qrRepository.findByUserId(42L)).thenReturn(List.of(q));

        SceneOutput output = scene.handle(
            new CallbackData("report", "start", List.of()),
            new TelegramUpdateContext(100L, 42L, new User(), null));

        assertThat(output.text()).contains("Выберите метку");
        assertThat(output.editInPlace()).isTrue();
    }

    @Test
    void start_emptyActiveQrs_rendersEmptyMessage() {
        Qr deleted = new Qr();
        deleted.setId(UUID.randomUUID());
        deleted.setStatus(QrStatus.DELETED);
        when(qrRepository.findByUserId(42L)).thenReturn(List.of(deleted));

        SceneOutput output = scene.handle(
            new CallbackData("report", "start", List.of()),
            new TelegramUpdateContext(100L, 42L, new User(), null));

        assertThat(output.text()).contains("нет активных меток");
    }

    @Test
    void start_withQrId_rendersReasonList() {
        UUID qrId = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(qrId);
        qr.setName("Audi");
        qr.setSeqNumber(145L);
        qr.setStatus(QrStatus.ACTIVE);
        when(qrRepository.findById(qrId)).thenReturn(Optional.of(qr));
        when(reasonDictionaryService.findAll()).thenReturn(List.of(
            ReasonDictionaryDto.builder().id(7L).description("Сигналка").build()));

        SceneOutput output = scene.handle(
            new CallbackData("report", "start", List.of(qrId.toString())),
            new TelegramUpdateContext(100L, 42L, new User(), null));

        assertThat(output.text()).contains("Выберите причину");
        assertThat(output.text()).contains("Audi");
    }

    @Test
    void qr_selectsQrAndRendersReasons() {
        UUID qrId = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(qrId);
        qr.setName("Skoda");
        qr.setSeqNumber(10L);
        qr.setStatus(QrStatus.ACTIVE);
        when(qrRepository.findById(qrId)).thenReturn(Optional.of(qr));
        when(reasonDictionaryService.findAll()).thenReturn(List.of(
            ReasonDictionaryDto.builder().id(7L).description("Сигналка").build()));

        SceneOutput output = scene.handle(
            new CallbackData("report", "qr", List.of(qrId.toString())),
            new TelegramUpdateContext(100L, 42L, new User(), null));

        assertThat(output.text()).contains("Выберите причину");
        assertThat(output.text()).contains("Skoda");
    }

    @Test
    void reason_setsPendingStateAndAsksForText() {
        UUID qrId = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(qrId);
        qr.setName("A");
        qr.setSeqNumber(1L);
        when(qrRepository.findById(qrId)).thenReturn(Optional.of(qr));
        when(reasonDictionaryService.findAll()).thenReturn(List.of(
            ReasonDictionaryDto.builder().id(7L).description("Сигналка").build()));

        scene.handle(new CallbackData("report", "reason", List.of(qrId.toString(), "7")),
            new TelegramUpdateContext(100L, 42L, new User(), null));

        verify(stateRegistry).put(100L, "report", "text", List.of(qrId.toString(), "7"));
    }

    @Test
    void handleText_storesDraftAndRendersPreview() {
        UUID qrId = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(qrId);
        qr.setName("Audi");
        qr.setSeqNumber(145L);
        when(qrRepository.findById(qrId)).thenReturn(Optional.of(qr));
        when(reasonDictionaryService.findAll()).thenReturn(List.of(
            ReasonDictionaryDto.builder().id(7L).description("Сигналка").build()));

        SceneOutput output = scene.handleText("сработала сигналка",
            new TelegramUpdateContext(100L, 42L, new User(), null),
            List.of(qrId.toString(), "7"));

        verify(stateRegistry).updateDraft(100L, "сработала сигналка");
        assertThat(output.text()).contains("сработала сигналка");
        assertThat(output.text()).contains("Сигналка");
    }

    @Test
    void handleText_truncatesLongText() {
        UUID qrId = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(qrId);
        qr.setName("A");
        qr.setSeqNumber(1L);
        when(qrRepository.findById(qrId)).thenReturn(Optional.of(qr));
        when(reasonDictionaryService.findAll()).thenReturn(List.of(
            ReasonDictionaryDto.builder().id(7L).description("R").build()));
        String longText = "a".repeat(600);

        scene.handleText(longText,
            new TelegramUpdateContext(100L, 42L, new User(), null),
            List.of(qrId.toString(), "7"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(stateRegistry).updateDraft(eq(100L), captor.capture());
        assertThat(captor.getValue()).hasSize(500);
    }

    @Test
    void submit_sendsNotificationAndClearsState() {
        UUID qrId = UUID.randomUUID();
        SceneStateRegistry.PendingText pending = new SceneStateRegistry.PendingText(
            "report", "text", List.of(qrId.toString(), "7"), "сработала", Instant.now().plusSeconds(60));
        when(stateRegistry.peek(100L)).thenReturn(Optional.of(pending));

        SceneOutput output = scene.handle(
            new CallbackData("report", "submit", List.of()),
            new TelegramUpdateContext(100L, 42L, new User(), null));

        ArgumentCaptor<NotificationDto> captor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(notificationFacade).send(captor.capture());
        assertThat(captor.getValue().getQrId()).isEqualTo(qrId);
        assertThat(captor.getValue().getReasonId()).isEqualTo(7L);
        assertThat(captor.getValue().getText()).isEqualTo("сработала");
        assertThat(captor.getValue().getSenderId()).isEqualTo(42L);
        verify(stateRegistry).clear(100L);
        assertThat(output.text()).contains("Событие отправлено");
    }

    @Test
    void submit_noPendingState_returnsNoop() {
        when(stateRegistry.peek(100L)).thenReturn(Optional.empty());

        SceneOutput output = scene.handle(
            new CallbackData("report", "submit", List.of()),
            new TelegramUpdateContext(100L, 42L, new User(), null));

        assertThat(output).isEqualTo(SceneOutput.noop());
    }

    @Test
    void submit_onFailure_rendersError() {
        UUID qrId = UUID.randomUUID();
        SceneStateRegistry.PendingText pending = new SceneStateRegistry.PendingText(
            "report", "text", List.of(qrId.toString(), "7"), "x", Instant.now().plusSeconds(60));
        when(stateRegistry.peek(100L)).thenReturn(Optional.of(pending));
        when(notificationFacade.send(any())).thenThrow(new RuntimeException("boom"));

        SceneOutput output = scene.handle(
            new CallbackData("report", "submit", List.of()),
            new TelegramUpdateContext(100L, 42L, new User(), null));

        assertThat(output.text()).contains("Не удалось отправить");
    }

    @Test
    void editText_reinstatesPendingState() {
        UUID qrId = UUID.randomUUID();
        scene.handle(new CallbackData("report", "edit_text", List.of(qrId.toString(), "7")),
            new TelegramUpdateContext(100L, 42L, new User(), null));
        verify(stateRegistry).put(100L, "report", "text", List.of(qrId.toString(), "7"));
    }

    private static TelegramMessages messages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
