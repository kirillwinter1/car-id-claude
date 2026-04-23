package ru.car.service.message.telegram.scene.impl;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.dto.MarketplacesDto;
import ru.car.service.MarketplaceService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;

import java.util.ArrayList;
import java.util.List;

@Component
public class MarketplaceScene implements TelegramScene {

    public static final String KEY = "marketplace";
    public static final String ACTION_OPEN = "open";

    private final MarketplaceService marketplaceService;
    private final TelegramMessages messages;

    public MarketplaceScene(MarketplaceService marketplaceService, TelegramMessages messages) {
        this.marketplaceService = marketplaceService;
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        if (ACTION_OPEN.equals(data.action())) {
            return renderOpen();
        }
        return SceneOutput.noop();
    }

    private SceneOutput renderOpen() {
        MarketplacesDto dto = marketplaceService.get();
        boolean wbEmpty = dto.getWb() == null || dto.getWb().isBlank();
        boolean ozonEmpty = dto.getOzon() == null || dto.getOzon().isBlank();
        if (!Boolean.TRUE.equals(dto.getActivity()) || (wbEmpty && ozonEmpty)) {
            InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
            kb.setKeyboard(List.of(List.of(
                btnCallback(messages.get("tg.common.back"), "marketplace:back"),
                btnCallback(messages.get("tg.common.home"), "home:open")
            )));
            return SceneOutput.editHtml(messages.get("tg.marketplace.unavailable"), kb);
        }

        String body = messages.get("tg.marketplace.title") + "\n\n" + messages.get("tg.marketplace.body");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        if (!wbEmpty) rows.add(List.of(btnUrl(messages.get("tg.marketplace.btn.wb"), dto.getWb())));
        if (!ozonEmpty) rows.add(List.of(btnUrl(messages.get("tg.marketplace.btn.ozon"), dto.getOzon())));
        rows.add(List.of(
            btnCallback(messages.get("tg.common.back"), "marketplace:back"),
            btnCallback(messages.get("tg.common.home"), "home:open")
        ));
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        return SceneOutput.editHtml(body, kb);
    }

    private static InlineKeyboardButton btnCallback(String text, String cb) {
        return InlineKeyboardButton.builder().text(text).callbackData(cb).build();
    }

    private static InlineKeyboardButton btnUrl(String text, String url) {
        return InlineKeyboardButton.builder().text(text).url(url).build();
    }
}
