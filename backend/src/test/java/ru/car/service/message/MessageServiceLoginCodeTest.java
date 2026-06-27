package ru.car.service.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.car.service.AuthenticationCodeService;
import ru.car.service.message.telegram.gateway.TelegramGatewayService;
import ru.car.service.message.zvonok.ZvonokService;
import ru.car.test.base.BaseUnitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MessageService.sendLoginCode Tests")
class MessageServiceLoginCodeTest extends BaseUnitTest {

    @Mock private TelegramGatewayService telegramGatewayService;
    @Mock private ZvonokService zvonokService;
    @Mock private AuthenticationCodeService authenticationCodeService;

    @InjectMocks private MessageService messageService;

    @Test
    @DisplayName("используем Telegram, когда код доставим")
    void usesTelegramWhenDeliverable() {
        when(telegramGatewayService.isEnabled()).thenReturn(true);
        when(telegramGatewayService.checkSendAbility("79991234567")).thenReturn("req-1");
        when(telegramGatewayService.sendCode(eq("79991234567"), anyString(), eq("req-1"))).thenReturn(true);

        MessageService.LoginCodeResult result = messageService.sendLoginCode("79991234567");

        assertThat(result.channel()).isEqualTo("telegram");
        assertThat(result.code()).hasSize(4);
        verify(zvonokService, never()).sendCode(any());
    }

    @Test
    @DisplayName("фолбэк на flashcall, когда Telegram не доставит")
    void fallsBackWhenNotDeliverable() {
        when(telegramGatewayService.isEnabled()).thenReturn(true);
        when(telegramGatewayService.checkSendAbility(anyString())).thenReturn(null);
        when(zvonokService.sendCode("79991234567")).thenReturn("4321");

        MessageService.LoginCodeResult result = messageService.sendLoginCode("79991234567");

        assertThat(result.channel()).isEqualTo("call");
        assertThat(result.code()).isEqualTo("4321");
    }

    @Test
    @DisplayName("фолбэк, когда Gateway выключен (checkSendAbility не зовём)")
    void fallsBackWhenGatewayDisabled() {
        when(telegramGatewayService.isEnabled()).thenReturn(false);
        when(zvonokService.sendCode("79991234567")).thenReturn("4321");

        MessageService.LoginCodeResult result = messageService.sendLoginCode("79991234567");

        assertThat(result.channel()).isEqualTo("call");
        verify(telegramGatewayService, never()).checkSendAbility(anyString());
    }
}
