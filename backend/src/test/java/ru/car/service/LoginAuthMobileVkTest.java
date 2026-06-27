package ru.car.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.car.dto.login_oauth_code.LoginAuthCodeRsParams;
import ru.car.dto.login_vk.LoginVkRqParams;
import ru.car.enums.Role;
import ru.car.exception.BadRequestException;
import ru.car.model.User;
import ru.car.model.security.SecurityUser;
import ru.car.service.security.JwtService;
import ru.car.service.security.SecurityUserService;
import ru.car.service.vk.VkIdService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAuthMobileService.loginVk Tests")
class LoginAuthMobileVkTest {

    @Mock private VkIdService vkIdService;
    @Mock private UserService userService;
    @Mock private JwtService jwtService;
    @Mock private SecurityUserService securityUserService;

    @InjectMocks private LoginAuthMobileService service;

    @Test
    @DisplayName("loginVk: телефон из VK → JWT")
    void loginVkReturnsToken() {
        User user = User.builder().id(1L).phoneNumber("79991234567").build();
        SecurityUser details = SecurityUser.builder().id(1L).telephone("79991234567").build();
        when(vkIdService.isEnabled()).thenReturn(true);
        when(vkIdService.fetchVerifiedPhone("tok")).thenReturn("79991234567");
        when(userService.findOrCreateByPhoneNumberAndActivate("79991234567", Role.ROLE_USER)).thenReturn(user);
        when(securityUserService.getDetails(user)).thenReturn(details);
        when(jwtService.generateToken(details)).thenReturn("JWT");

        LoginAuthCodeRsParams rs = service.loginVk(LoginVkRqParams.builder().accessToken("tok").build());

        assertThat(rs.getToken()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("loginVk: при выключенном VK — ошибка")
    void loginVkDisabled() {
        when(vkIdService.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.loginVk(LoginVkRqParams.builder().accessToken("tok").build()))
                .isInstanceOf(BadRequestException.class);
    }
}
