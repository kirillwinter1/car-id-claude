package ru.car.service.message.telegram.scene.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.enums.QrStatus;
import ru.car.model.Qr;
import ru.car.repository.QrRepository;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.util.MessageUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class QrListScene implements TelegramScene {

    public static final String KEY = "qr_list";
    public static final String ACTION_OPEN = "open";

    private static final Locale RU = new Locale("ru", "RU");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy", RU);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final QrRepository qrRepository;
    private final TelegramMessages messages;

    public QrListScene(QrRepository qrRepository, TelegramMessages messages) {
        this.qrRepository = qrRepository;
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public boolean canHandleText(String text) {
        return "QR-коды".equals(text);
    }

    @Override
    public SceneOutput render(TelegramUpdateContext ctx) {
        return build(ctx.userId(), false);
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        if (!ACTION_OPEN.equals(data.action())) {
            return SceneOutput.noop();
        }
        return build(ctx.userId(), true);
    }

    private SceneOutput build(Long userId, boolean edit) {
        List<Qr> qrs = qrRepository.findByUserId(userId);
        List<Qr> visible = qrs.stream()
            .filter(qr -> !QrStatus.DELETED.equals(qr.getStatus()))
            .toList();

        if (CollectionUtils.isEmpty(visible)) {
            return buildEmpty(edit);
        }

        StringBuilder body = new StringBuilder(messages.get("tg.qr.list.title"));
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Qr qr : visible) {
            body.append("\n\n").append(formatCard(qr));
            rows.add(row(shortName(qr), "qr_details:open:" + qr.getId()));
        }
        rows.add(row(messages.get("tg.common.back"), "qr_list:back"));

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        return edit ? SceneOutput.editHtml(body.toString(), kb) : SceneOutput.sendHtml(body.toString(), kb);
    }

    private SceneOutput buildEmpty(boolean edit) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row(messages.get("tg.home.btn.temp_qr"), "temp_qr:create"));
        rows.add(List.of(
            InlineKeyboardButton.builder()
                .text(messages.get("tg.qr.list.btn.ozon"))
                .url("https://ozon.ru/t/gXqGlgj")
                .build()
        ));
        rows.add(row(messages.get("tg.common.back"), "qr_list:back"));
        kb.setKeyboard(rows);
        String body = messages.get("tg.qr.list.empty_v2");
        return edit ? SceneOutput.editHtml(body, kb) : SceneOutput.sendHtml(body, kb);
    }

    private String formatCard(Qr qr) {
        String name = MessageUtils.escapeHtml(qr.getName() != null ? qr.getName() : "");
        String emoji = statusEmoji(qr.getStatus());
        return switch (qr.getStatus()) {
            case TEMPORARY -> {
                String expires = qr.getCreatedDate() != null
                    ? TIME_FMT.format(qr.getCreatedDate().plusHours(1))
                    : "-";
                yield messages.get("tg.qr.list.card.temp", expires);
            }
            case DELETED -> messages.get("tg.qr.list.card.disabled", name, qr.getSeqNumber());
            case ACTIVE, NEW -> messages.get("tg.qr.list.card.active",
                emoji,
                name,
                qr.getSeqNumber(),
                qr.getActivateDate() != null ? DATE_FMT.format(qr.getActivateDate()) : "-",
                0);
        };
    }

    private String shortName(Qr qr) {
        String emoji = statusEmoji(qr.getStatus());
        String name = qr.getName() != null ? qr.getName() : "Временная";
        return emoji + " " + name;
    }

    private String statusEmoji(QrStatus s) {
        return switch (s) {
            case ACTIVE -> "🟢";
            case DELETED -> "⏸";
            case TEMPORARY -> "🕐";
            case NEW -> "⚪";
        };
    }

    private static List<InlineKeyboardButton> row(String text, String callback) {
        return List.of(InlineKeyboardButton.builder().text(text).callbackData(callback).build());
    }
}
