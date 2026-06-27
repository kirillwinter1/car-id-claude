package ru.car.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.car.dto.login_auth_mobile.LoginAuthMobileRqParams;
import ru.car.dto.login_auth_mobile.LoginAuthMobileRsParams;
import ru.car.service.message.MessageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAuthMobileService Tests")
class LoginAuthMobileServiceTest {

    @Mock private MessageService messageService;
    @Mock private AuthenticationCodeService authenticationCodeService;

    @InjectMocks private LoginAuthMobileService service;

    @Test
    @DisplayName("loginMobile: шлёт код через sendLoginCode, сохраняет его и отдаёт channel")
    void loginMobileSendsAndStoresCode() {
        ReflectionTestUtils.setField(service, "adminPhone", "70000000000");
        LoginAuthMobileRqParams params = new LoginAuthMobileRqParams();
        params.setPhoneNumber("79991234567");
        when(authenticationCodeService.isAlreadySent("79991234567")).thenReturn(false);
        when(messageService.sendLoginCode("79991234567"))
                .thenReturn(new MessageService.LoginCodeResult("1234", "telegram"));

        LoginAuthMobileRsParams rs = service.loginMobile(params);

        verify(authenticationCodeService).create("79991234567", "1234");
        assertThat(rs.getChannel()).isEqualTo("telegram");
    }
}
