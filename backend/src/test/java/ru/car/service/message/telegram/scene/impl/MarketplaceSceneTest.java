package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import ru.car.dto.MarketplacesDto;
import ru.car.service.MarketplaceService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceSceneTest {

    @Mock MarketplaceService marketplaceService;

    MarketplaceScene scene;

    @BeforeEach
    void setUp() {
        scene = new MarketplaceScene(marketplaceService, messages());
    }

    @Test
    void keyIsMarketplace() {
        assertThat(scene.key()).isEqualTo("marketplace");
    }

    @Test
    void open_whenActiveWithBothUrls_rendersBothButtons() {
        MarketplacesDto dto = MarketplacesDto.builder()
            .wb("https://wildberries.ru/X").ozon("https://ozon.ru/Y").activity(true).build();
        when(marketplaceService.get()).thenReturn(dto);

        SceneOutput output = scene.handle(
            new CallbackData("marketplace", "open", List.of()),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).contains("Заказать QR-стикеры");
        assertThat(output.inlineKeyboard()).isNotNull();
        assertThat(output.editInPlace()).isTrue();
    }

    @Test
    void open_whenInactive_rendersUnavailable() {
        MarketplacesDto dto = MarketplacesDto.builder().activity(false).build();
        when(marketplaceService.get()).thenReturn(dto);

        SceneOutput output = scene.handle(
            new CallbackData("marketplace", "open", List.of()),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).contains("Пока недоступно");
    }

    @Test
    void open_whenBothUrlsEmpty_rendersUnavailable() {
        MarketplacesDto dto = MarketplacesDto.builder().activity(true).wb("").ozon(null).build();
        when(marketplaceService.get()).thenReturn(dto);

        SceneOutput output = scene.handle(
            new CallbackData("marketplace", "open", List.of()),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).contains("Пока недоступно");
    }

    private static TelegramMessages messages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
