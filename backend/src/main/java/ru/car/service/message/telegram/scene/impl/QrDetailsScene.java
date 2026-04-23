package ru.car.service.message.telegram.scene.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.enums.QrStatus;
import ru.car.model.Qr;
import ru.car.service.NotificationService;
import ru.car.service.QrService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.util.MessageUtils;
import ru.car.util.QrUtils;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
public class QrDetailsScene implements TelegramScene {

    public static final String KEY = "qr_details";
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_SHOW = "show";
    public static final String ACTION_DISABLE = "disable";
    public static final String ACTION_DISABLE_CONFIRM = "disable_confirm";

    private static final Locale RU = new Locale("ru", "RU");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy", RU);

    private final QrService qrService;
    private final NotificationService notificationService;
    private final TelegramMessages messages;

    @Value("${swagger.server.url}")
    private String serverUrl;

    public QrDetailsScene(QrService qrService, NotificationService notificationService, TelegramMessages messages) {
        this.qrService = qrService;
        this.notificationService = notificationService;
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String parentKey() {
        return "qr_list";
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        if (data.args().isEmpty() || !MessageUtils.isUUID(data.args().get(0))) {
            return SceneOutput.noop();
        }
        UUID id = UUID.fromString(data.args().get(0));

        return switch (data.action()) {
            case ACTION_OPEN -> renderDetails(id);
            case ACTION_SHOW -> renderPhoto(id);
            case ACTION_DISABLE -> renderDisableConfirm(id);
            case ACTION_DISABLE_CONFIRM -> {
                try {
                    qrService.disable(id);
                } catch (Exception e) {
                    log.warn("disable failed for {}", id, e);
                }
                yield renderDetails(id);
            }
            default -> SceneOutput.noop();
        };
    }

    private SceneOutput renderDetails(UUID id) {
        Qr qr = qrService.findByIdOrThrowNotFound(id);
        String emoji = statusEmoji(qr.getStatus());
        String name = MessageUtils.escapeHtml(qr.getName() != null ? qr.getName() : "Метка");
        String statusText = statusText(qr.getStatus());
        String activated = qr.getActivateDate() != null ? DATE_FMT.format(qr.getActivateDate()) : "-";
        int totalNotif = 0;
        int unreadNotif = 0;

        String title = messages.get("tg.qr.details.title", emoji, name);
        String fields = messages.get("tg.qr.details.fields",
            qr.getSeqNumber(),
            emoji + " " + statusText,
            activated,
            totalNotif,
            unreadNotif);
        String body = title + "\n\n" + fields;

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(
            List.of(btn(messages.get("tg.qr.details.btn.show"), "qr_details:show:" + id)),
            List.of(btn(messages.get("tg.qr.details.btn.history"), "notif_list:open:all:1:qr:" + id)),
            List.of(btn(messages.get("tg.qr.details.btn.report"), "report:start:" + id)),
            List.of(btn(messages.get("tg.qr.details.btn.disable"), "qr_details:disable:" + id)),
            List.of(
                btn(messages.get("tg.qr.details.btn.back_to_list"), "qr_details:back"),
                btn(messages.get("tg.common.home"), "home:open")
            )
        ));
        return SceneOutput.editHtml(body, kb);
    }

    private SceneOutput renderPhoto(UUID id) {
        Qr qr = qrService.findByIdOrThrowNotFound(id);
        String url = QrUtils.getQrUrlById(serverUrl, id);
        try {
            Resource pngResource = QrUtils.generateQRCodeImage(url);
            InputFile input = new InputFile(pngResource.getInputStream(), "qr-" + id + ".png");
            String name = MessageUtils.escapeHtml(qr.getName() != null ? qr.getName() : "Метка");
            String caption = "<b>" + name + "</b> · №" + qr.getSeqNumber();
            return SceneOutput.photo(input, caption);
        } catch (IOException e) {
            log.error("Failed to generate QR image for {}", id, e);
            return SceneOutput.noop();
        }
    }

    private SceneOutput renderDisableConfirm(UUID id) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(
            List.of(btn(messages.get("tg.common.confirm.yes"), "qr_details:disable_confirm:" + id)),
            List.of(btn(messages.get("tg.common.confirm.cancel"), "qr_details:open:" + id))
        ));
        return SceneOutput.editHtml(messages.get("tg.qr.details.confirm.disable"), kb);
    }

    private String statusText(QrStatus s) {
        return switch (s) {
            case ACTIVE -> messages.get("tg.qr.details.status.active");
            case DELETED -> messages.get("tg.qr.details.status.disabled");
            case TEMPORARY -> messages.get("tg.qr.details.status.temp");
            case NEW -> messages.get("tg.qr.details.status.new");
        };
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
