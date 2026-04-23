package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import ru.car.dto.FeedbackDto;
import ru.car.enums.FeedbackChannels;
import ru.car.model.User;
import ru.car.service.FeedbackFacade;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.state.SceneStateRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SupportSceneTest {

    @Mock FeedbackFacade feedbackFacade;
    @Mock SceneStateRegistry stateRegistry;

    SupportScene scene;

    @BeforeEach
    void setUp() {
        scene = new SupportScene(feedbackFacade, stateRegistry, messages());
    }

    @Test
    void keyIsSupport() {
        assertThat(scene.key()).isEqualTo("support");
    }

    @Test
    void start_rendersPromptAndSetsPendingState() {
        SceneOutput output = scene.handle(
            new CallbackData("support", "start", List.of()),
            new TelegramUpdateContext(100L, 42L, new User(), null));

        assertThat(output.text()).contains("Опишите проблему");
        assertThat(output.editInPlace()).isTrue();
        verify(stateRegistry).put(100L, "support", "text", List.of());
    }

    @Test
    void handleText_sendsFeedbackAndClearsState() {
        User u = new User();
        u.setPhoneNumber("79001234567");

        SceneOutput output = scene.handleText("help me please",
            new TelegramUpdateContext(100L, 42L, u, null), List.of());

        ArgumentCaptor<FeedbackDto> captor = ArgumentCaptor.forClass(FeedbackDto.class);
        verify(feedbackFacade).send(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("help me please");
        assertThat(captor.getValue().getChannel()).isEqualTo(FeedbackChannels.TELEGRAM);
        assertThat(captor.getValue().getEmail()).contains("79001234567");
        verify(stateRegistry).clear(eq(100L));
        assertThat(output.text()).contains("Спасибо");
    }

    @Test
    void handleText_whenFeedbackFails_stillRendersThankYouAndClears() {
        User u = new User();
        u.setPhoneNumber("79001234567");
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(feedbackFacade).send(org.mockito.ArgumentMatchers.any());

        SceneOutput output = scene.handleText("help",
            new TelegramUpdateContext(100L, 42L, u, null), List.of());

        verify(stateRegistry).clear(100L);
        assertThat(output.text()).contains("Спасибо");
    }

    private static TelegramMessages messages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
