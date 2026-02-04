package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.car.dto.login_oauth_code.LoginAuthCodeRqParams;
import ru.car.dto.login_oauth_code.LoginAuthCodeRsParams;
import ru.car.model.User;
import ru.car.service.message.MessageService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoginAuthMobileFacade {
    private final LoginAuthMobileService loginAuthMobileService;
    private final UserService userService;
    private final MessageService messageService;

    public LoginAuthCodeRsParams confirmCode(LoginAuthCodeRqParams params) {
        Optional<User> user = userService.findByPhoneNumber(params.getPhoneNumber());

        LoginAuthCodeRsParams loginAuthCodeRsParams = loginAuthMobileService.confirmCode(params);
        if (user.isEmpty()) {
            messageService.sendHelloNewUser(params.getPhoneNumber());
        }
        return loginAuthCodeRsParams;

    }
}
