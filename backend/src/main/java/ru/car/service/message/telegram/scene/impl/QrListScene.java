package ru.car.service.message.telegram.scene.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.enums.BatchTemplates;
import ru.car.exception.MessageNotSendException;
import ru.car.model.Qr;
import ru.car.repository.BatchRepository;
import ru.car.repository.QrRepository;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.service.message.telegram.transport.TelegramTransport;
import ru.car.util.MessageUtils;
import ru.car.util.SvgUtils;
import ru.car.util.svg.PdfConverter;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class QrListScene implements TelegramScene {

    public static final String KEY = "qr";
    public static final String ACTION_LIST = "list";
    public static final String ACTION_PDF = "pdf";

    private final QrRepository qrRepository;
    private final BatchRepository batchRepository;
    private final TelegramTransport transport;
    private final Executor pushExecutor;
    private final TelegramMessages messages;
    private final HomeMenuScene homeMenuScene;

    public QrListScene(QrRepository qrRepository,
                       BatchRepository batchRepository,
                       TelegramTransport transport,
                       @Qualifier("pushExecutorService") Executor pushExecutor,
                       TelegramMessages messages,
                       HomeMenuScene homeMenuScene) {
        this.qrRepository = qrRepository;
        this.batchRepository = batchRepository;
        this.transport = transport;
        this.pushExecutor = pushExecutor;
        this.messages = messages;
        this.homeMenuScene = homeMenuScene;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public boolean canHandleText(String text) {
        return messages.get("tg.home.btn.qrs").equals(text);
    }

    @Override
    public SceneOutput render(TelegramUpdateContext ctx) {
        List<Qr> qrs = qrRepository.findByUserId(ctx.userId());
        if (CollectionUtils.isEmpty(qrs)) {
            return SceneOutput.send(messages.get("tg.qr.list.empty"), homeMenuScene.mainKeyboard());
        }
        return SceneOutput.sendWithInline(messages.get("tg.qr.list.title"), buildQrKeyboard(qrs));
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        return switch (data.action()) {
            case ACTION_LIST -> handleList(ctx);
            case ACTION_PDF -> handlePdf(data, ctx);
            default -> SceneOutput.noop();
        };
    }

    private SceneOutput handleList(TelegramUpdateContext ctx) {
        List<Qr> qrs = qrRepository.findByUserId(ctx.userId());
        return SceneOutput.editMarkup(buildQrKeyboard(qrs));
    }

    private SceneOutput handlePdf(CallbackData data, TelegramUpdateContext ctx) {
        if (data.args().isEmpty() || !MessageUtils.isUUID(data.args().get(0))) {
            return SceneOutput.noop();
        }
        UUID id = UUID.fromString(data.args().get(0));
        BatchTemplates template = batchRepository.findByQrId(id)
                .map(b -> b.getTemplate())
                .orElse(BatchTemplates.PT_WHITE_1);
        long chatId = ctx.chatId();
        pushExecutor.execute(() -> sendPdf(chatId, id, template));
        return SceneOutput.noop();
    }

    private void sendPdf(long chatId, UUID id, BatchTemplates template) {
        try {
            SendDocument document = SendDocument.builder()
                    .chatId(chatId)
                    .document(new InputFile(
                            new ByteArrayInputStream(PdfConverter.create(SvgUtils.getSvgText(id.toString(), template))),
                            "qr.pdf"))
                    .build();
            transport.sendDocument(document);
            log.info("Отправил \"qr.pdf\" {}", id);
        } catch (Exception e) {
            log.error("Ошибка при отправке \"qr.pdf\" {}", id, e);
            throw new MessageNotSendException(e);
        }
    }

    private InlineKeyboardMarkup buildQrKeyboard(List<Qr> qrs) {
        if (CollectionUtils.isEmpty(qrs)) {
            return null;
        }
        List<List<InlineKeyboardButton>> rows = qrs.stream()
                .map(qr -> List.of(InlineKeyboardButton.builder()
                        .text(qr.getName() + " " + qr.getSeqNumber())
                        .callbackData(new CallbackData(KEY, ACTION_PDF, List.of(qr.getId().toString())).serialize())
                        .build()))
                .toList();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
}
