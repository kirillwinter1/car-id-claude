package ru.car.service.message.telegram.scene.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.dto.NotificationDto;
import ru.car.dto.ReasonDictionaryDto;
import ru.car.enums.QrStatus;
import ru.car.model.Qr;
import ru.car.repository.QrRepository;
import ru.car.service.NotificationFacade;
import ru.car.service.ReasonDictionaryService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.service.message.telegram.scene.state.SceneStateRegistry;
import ru.car.util.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ReportEventScene implements TelegramScene {

    public static final String KEY = "report";
    public static final String ACTION_START = "start";
    public static final String ACTION_QR = "qr";
    public static final String ACTION_REASON = "reason";
    public static final String ACTION_EDIT_TEXT = "edit_text";
    public static final String ACTION_SUBMIT = "submit";

    private static final int MAX_TEXT = 500;

    private final NotificationFacade notificationFacade;
    private final QrRepository qrRepository;
    private final ReasonDictionaryService reasonDictionaryService;
    private final SceneStateRegistry stateRegistry;
    private final TelegramMessages messages;

    public ReportEventScene(@Lazy NotificationFacade notificationFacade,
                             QrRepository qrRepository,
                             ReasonDictionaryService reasonDictionaryService,
                             SceneStateRegistry stateRegistry,
                             TelegramMessages messages) {
        this.notificationFacade = notificationFacade;
        this.qrRepository = qrRepository;
        this.reasonDictionaryService = reasonDictionaryService;
        this.stateRegistry = stateRegistry;
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        return switch (data.action()) {
            case ACTION_START -> {
                if (!data.args().isEmpty() && MessageUtils.isUUID(data.args().get(0))) {
                    yield renderReasons(UUID.fromString(data.args().get(0)));
                }
                yield renderQrList(ctx.userId());
            }
            case ACTION_QR -> {
                if (data.args().isEmpty() || !MessageUtils.isUUID(data.args().get(0))) {
                    yield SceneOutput.noop();
                }
                yield renderReasons(UUID.fromString(data.args().get(0)));
            }
            case ACTION_REASON -> {
                if (data.args().size() < 2 || !MessageUtils.isUUID(data.args().get(0))) {
                    yield SceneOutput.noop();
                }
                String qrArg = data.args().get(0);
                String reasonArg = data.args().get(1);
                stateRegistry.put(ctx.chatId(), KEY, "text", List.of(qrArg, reasonArg));
                yield renderEnterText(UUID.fromString(qrArg), Long.parseLong(reasonArg));
            }
            case ACTION_EDIT_TEXT -> {
                if (data.args().size() < 2 || !MessageUtils.isUUID(data.args().get(0))) {
                    yield SceneOutput.noop();
                }
                String qrArg = data.args().get(0);
                String reasonArg = data.args().get(1);
                stateRegistry.put(ctx.chatId(), KEY, "text", List.of(qrArg, reasonArg));
                yield renderEnterText(UUID.fromString(qrArg), Long.parseLong(reasonArg));
            }
            case ACTION_SUBMIT -> submit(ctx);
            default -> SceneOutput.noop();
        };
    }

    @Override
    public SceneOutput handleText(String text, TelegramUpdateContext ctx, List<String> args) {
        if (args.size() < 2 || !MessageUtils.isUUID(args.get(0))) {
            return SceneOutput.noop();
        }
        String truncated = text != null && text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
        stateRegistry.updateDraft(ctx.chatId(), truncated);
        UUID qrId = UUID.fromString(args.get(0));
        long reasonId = Long.parseLong(args.get(1));
        return renderPreview(qrId, reasonId, truncated);
    }

    private SceneOutput renderQrList(Long userId) {
        List<Qr> qrs = qrRepository.findByUserId(userId).stream()
            .filter(q -> QrStatus.ACTIVE.equals(q.getStatus()) || QrStatus.TEMPORARY.equals(q.getStatus()))
            .toList();
        if (qrs.isEmpty()) {
            InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
            kb.setKeyboard(List.of(List.of(btn(messages.get("tg.common.home"), "home:open"))));
            return SceneOutput.editHtml(messages.get("tg.report.empty_qrs"), kb);
        }
        String body = messages.get("tg.report.title") + "\n\n" + messages.get("tg.report.choose_qr");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Qr qr : qrs) {
            String label = statusEmoji(qr.getStatus()) + " "
                + MessageUtils.escapeHtml(qr.getName() != null ? qr.getName() : "Метка")
                + " · №" + qr.getSeqNumber();
            rows.add(List.of(btn(label, "report:qr:" + qr.getId())));
        }
        rows.add(List.of(
            btn(messages.get("tg.common.back"), "report:back"),
            btn(messages.get("tg.common.home"), "home:open")
        ));
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        return SceneOutput.editHtml(body, kb);
    }

    private SceneOutput renderReasons(UUID qrId) {
        Optional<Qr> qrOpt = qrRepository.findById(qrId);
        if (qrOpt.isEmpty()) return SceneOutput.noop();
        Qr qr = qrOpt.get();
        String qrLine = "Метка: <b>" + MessageUtils.escapeHtml(qr.getName() != null ? qr.getName() : "Метка")
            + "</b> · №" + qr.getSeqNumber();
        String body = messages.get("tg.report.title") + "\n" + qrLine + "\n\n" + messages.get("tg.report.choose_reason");

        List<ReasonDictionaryDto> reasons = reasonDictionaryService.findAll();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (ReasonDictionaryDto r : reasons) {
            rows.add(List.of(btn(r.getDescription(), "report:reason:" + qrId + ":" + r.getId())));
        }
        rows.add(List.of(
            btn(messages.get("tg.report.btn.back_to_qr"), "report:start"),
            btn(messages.get("tg.common.home"), "home:open")
        ));
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        return SceneOutput.editHtml(body, kb);
    }

    private SceneOutput renderEnterText(UUID qrId, long reasonId) {
        Optional<Qr> qrOpt = qrRepository.findById(qrId);
        if (qrOpt.isEmpty()) return SceneOutput.noop();
        Qr qr = qrOpt.get();
        String reasonName = resolveReason(reasonId);
        String qrLine = "Метка: <b>" + MessageUtils.escapeHtml(qr.getName() != null ? qr.getName() : "Метка")
            + "</b> · №" + qr.getSeqNumber();
        String reasonLine = "Причина: <b>" + MessageUtils.escapeHtml(reasonName) + "</b>";
        String body = messages.get("tg.report.title") + "\n" + qrLine + "\n" + reasonLine + "\n\n"
            + messages.get("tg.report.enter_text");

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(List.of(
            btn(messages.get("tg.report.btn.change_reason"), "report:qr:" + qrId),
            btn(messages.get("tg.common.home"), "home:open")
        )));
        return SceneOutput.editHtml(body, kb);
    }

    private SceneOutput renderPreview(UUID qrId, long reasonId, String draftText) {
        Optional<Qr> qrOpt = qrRepository.findById(qrId);
        if (qrOpt.isEmpty()) return SceneOutput.noop();
        Qr qr = qrOpt.get();
        String reasonName = resolveReason(reasonId);
        String body = messages.get("tg.report.title") + "\n\n"
            + messages.get("tg.report.preview.body",
                MessageUtils.escapeHtml(qr.getName() != null ? qr.getName() : "Метка"),
                qr.getSeqNumber(),
                MessageUtils.escapeHtml(reasonName),
                MessageUtils.escapeHtml(draftText != null ? draftText : ""));

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(
            List.of(btn(messages.get("tg.report.btn.submit"), "report:submit")),
            List.of(btn(messages.get("tg.report.btn.edit_text"), "report:edit_text:" + qrId + ":" + reasonId)),
            List.of(btn(messages.get("tg.report.btn.change_reason"), "report:qr:" + qrId)),
            List.of(btn(messages.get("tg.common.home"), "home:open"))
        ));
        return SceneOutput.sendHtml(body, kb);
    }

    private SceneOutput submit(TelegramUpdateContext ctx) {
        Optional<SceneStateRegistry.PendingText> pending = stateRegistry.peek(ctx.chatId());
        if (pending.isEmpty() || pending.get().args().size() < 2) {
            return SceneOutput.noop();
        }
        String qrArg = pending.get().args().get(0);
        String reasonArg = pending.get().args().get(1);
        String draft = pending.get().draftText() != null ? pending.get().draftText() : "";
        try {
            NotificationDto dto = NotificationDto.builder()
                .qrId(UUID.fromString(qrArg))
                .reasonId(Long.parseLong(reasonArg))
                .text(draft)
                .senderId(ctx.userId())
                .build();
            notificationFacade.send(dto);
            stateRegistry.clear(ctx.chatId());
            InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
            kb.setKeyboard(List.of(List.of(btn(messages.get("tg.common.home"), "home:open"))));
            return SceneOutput.editHtml(messages.get("tg.report.success"), kb);
        } catch (Exception e) {
            log.error("report submit failed", e);
            InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
            kb.setKeyboard(List.of(List.of(
                btn(messages.get("tg.report.btn.edit_text"), "report:edit_text:" + qrArg + ":" + reasonArg),
                btn(messages.get("tg.common.home"), "home:open")
            )));
            return SceneOutput.editHtml(messages.get("tg.report.error", e.getMessage() != null ? e.getMessage() : ""), kb);
        }
    }

    private String resolveReason(long reasonId) {
        return reasonDictionaryService.findAll().stream()
            .filter(r -> r.getId() != null && r.getId() == reasonId)
            .map(ReasonDictionaryDto::getDescription)
            .findFirst()
            .orElse("—");
    }

    private String statusEmoji(QrStatus s) {
        return switch (s) {
            case ACTIVE -> "🟢";
            case DELETED -> "⏸";
            case TEMPORARY -> "🕐";
            case NEW -> "⚪";
        };
    }

    private static InlineKeyboardButton btn(String text, String callback) {
        return InlineKeyboardButton.builder().text(text).callbackData(callback).build();
    }
}
