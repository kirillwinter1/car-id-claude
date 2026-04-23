package ru.car.service.message.telegram.router;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.car.model.User;
import ru.car.repository.NotificationSettingRepository;
import ru.car.service.UserService;
import ru.car.service.message.telegram.auth.TelegramAuthorizationService;
import ru.car.service.message.telegram.render.TelegramRenderer;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.SceneRegistry;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.service.message.telegram.scene.impl.HomeScene;
import ru.car.service.message.telegram.scene.state.SceneStateRegistry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramRouterTest {

    @Mock NotificationSettingRepository settingRepository;
    @Mock UserService userService;
    @Mock SceneRegistry sceneRegistry;
    @Mock TelegramAuthorizationService authService;
    @Mock HomeScene homeScene;
    @Mock TelegramRenderer renderer;
    @Mock SceneStateRegistry sceneStateRegistry;
    @Mock TelegramScene matchedScene;

    private TelegramRouter router;

    @BeforeEach
    void setUp() {
        router = new TelegramRouter(settingRepository, userService, sceneRegistry, authService, homeScene, renderer, sceneStateRegistry);
    }

    @Test
    void noopOnEmptyUpdate() {
        router.route(new Update());
        verify(renderer, never()).dispatch(any(), anyLong(), any());
    }

    @Test
    void delegatesToAuthService_whenNotLinked() {
        Update update = textUpdate(100L, "hello");
        when(settingRepository.existsByTelegramDialogId(100L)).thenReturn(false);
        when(authService.handle(100L, "hello")).thenReturn(SceneOutput.send("please share contact", null));

        router.route(update);

        verify(renderer).dispatch(any(SceneOutput.class), eq(100L), any());
    }

    @Test
    void authService_usesContactPhoneWhenPresent() {
        Update update = contactUpdate(100L, "+79001234567");
        when(settingRepository.existsByTelegramDialogId(100L)).thenReturn(false);
        when(authService.handle(100L, "+79001234567")).thenReturn(SceneOutput.send("welcome", null));

        router.route(update);

        verify(authService).handle(100L, "+79001234567");
    }

    @Test
    void afterSuccessfulAuth_rendersHomeSceneAsSecondMessage() {
        Update update = contactUpdate(100L, "+79001234567");
        when(settingRepository.existsByTelegramDialogId(100L)).thenReturn(false, true);
        when(authService.handle(100L, "+79001234567")).thenReturn(SceneOutput.send("welcome", null));
        when(settingRepository.findUserIdByTelegramDialogId(100L)).thenReturn(42L);
        when(userService.getUserOrThrowNotFound(42L)).thenReturn(new User());
        TelegramScene homeScene = mock(TelegramScene.class);
        when(sceneRegistry.findByKey("home")).thenReturn(Optional.of(homeScene));
        when(homeScene.render(any())).thenReturn(SceneOutput.sendHtml("home", null));

        router.route(update);

        verify(homeScene).render(any());
        verify(renderer, times(2)).dispatch(any(SceneOutput.class), eq(100L), any());
    }

    @Test
    void routesCallbackToScene() {
        Update update = callbackUpdate(100L, "notif:read:550e8400-e29b-41d4-a716-446655440000");
        mockAuthorized(100L, 42L);
        when(sceneRegistry.findByKey("notif")).thenReturn(Optional.of(matchedScene));
        when(matchedScene.handle(any(CallbackData.class), any(TelegramUpdateContext.class)))
                .thenReturn(SceneOutput.editMarkup(null));

        router.route(update);

        verify(matchedScene).handle(any(CallbackData.class), any(TelegramUpdateContext.class));
    }

    @Test
    void unknownCallback_fallsBackToHomeUnknown() {
        Update update = callbackUpdate(100L, "junk:action");
        mockAuthorized(100L, 42L);
        when(sceneRegistry.findByKey("junk")).thenReturn(Optional.empty());
        when(homeScene.renderUnknown(any())).thenReturn(SceneOutput.send("Неизвестная команда", null));

        router.route(update);

        verify(homeScene).renderUnknown(any());
    }

    @Test
    void textTrigger_routesToMatchingScene() {
        Update update = textUpdate(100L, "QR-коды");
        mockAuthorized(100L, 42L);
        when(sceneRegistry.findByText("QR-коды")).thenReturn(Optional.of(matchedScene));
        when(matchedScene.render(any())).thenReturn(SceneOutput.send("list", null));

        router.route(update);

        verify(matchedScene).render(any());
    }

    @Test
    void routesBackCallback_toParentSceneViaEdit() {
        Update update = callbackUpdate(100L, "qr_details:back");
        mockAuthorized(100L, 42L);
        TelegramScene detailsScene = mock(TelegramScene.class);
        TelegramScene listScene = mock(TelegramScene.class);
        when(detailsScene.parentKey()).thenReturn("qr_list");
        when(sceneRegistry.findByKey("qr_details")).thenReturn(Optional.of(detailsScene));
        when(sceneRegistry.findByKey("qr_list")).thenReturn(Optional.of(listScene));
        when(listScene.render(any(TelegramUpdateContext.class))).thenReturn(SceneOutput.sendHtml("list", null));

        router.route(update);

        verify(listScene).render(any(TelegramUpdateContext.class));
        verify(renderer).dispatch(any(SceneOutput.class), eq(100L), any());
    }

    @Test
    void handleText_whenPendingState_delegatesToSceneHandleText() {
        Update update = textUpdate(100L, "hello");
        mockAuthorized(100L, 42L);
        SceneStateRegistry.PendingText pending = new SceneStateRegistry.PendingText(
            "report", "text", List.of("qr-id", "42"), null, Instant.now().plusSeconds(60));
        when(sceneStateRegistry.peek(100L)).thenReturn(Optional.of(pending));
        TelegramScene reportScene = mock(TelegramScene.class);
        when(sceneRegistry.findByKey("report")).thenReturn(Optional.of(reportScene));
        when(reportScene.handleText(eq("hello"), any(), eq(List.of("qr-id", "42"))))
            .thenReturn(SceneOutput.editHtml("done", null));

        router.route(update);

        verify(reportScene).handleText(eq("hello"), any(), eq(List.of("qr-id", "42")));
        verify(sceneRegistry, never()).findByText(any());
    }

    @Test
    void unknownText_fallsBackToHomeUnknown() {
        Update update = textUpdate(100L, "random text");
        mockAuthorized(100L, 42L);
        when(sceneRegistry.findByText("random text")).thenReturn(Optional.empty());
        when(homeScene.renderUnknown(any())).thenReturn(SceneOutput.send("Неизвестная команда", null));

        router.route(update);

        verify(homeScene).renderUnknown(any());
    }

    private void mockAuthorized(long chatId, long userId) {
        when(settingRepository.existsByTelegramDialogId(chatId)).thenReturn(true);
        when(settingRepository.findUserIdByTelegramDialogId(chatId)).thenReturn(userId);
        when(userService.getUserOrThrowNotFound(userId)).thenReturn(new User());
    }

    private static Update textUpdate(long chatId, String text) {
        Update update = new Update();
        Message message = new Message();
        message.setText(text);
        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);
        update.setMessage(message);
        return update;
    }

    private static Update contactUpdate(long chatId, String phone) {
        Update update = textUpdate(chatId, "");
        Contact contact = new Contact();
        contact.setPhoneNumber(phone);
        update.getMessage().setContact(contact);
        return update;
    }

    private static Update callbackUpdate(long chatId, String data) {
        Update update = new Update();
        CallbackQuery cb = new CallbackQuery();
        cb.setData(data);
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);
        cb.setMessage(message);
        update.setCallbackQuery(cb);
        return update;
    }
}
