package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.constants.ApplicationConstants;
import ru.car.dto.login_auth_mobile.LoginAuthMobileRqParams;
import ru.car.dto.login_auth_mobile.LoginAuthMobileRsParams;
import ru.car.dto.login_oauth_code.LoginAuthCodeRqParams;
import ru.car.dto.login_oauth_code.LoginAuthCodeRsParams;
import ru.car.enums.ErrorCode;
import ru.car.enums.Role;
import ru.car.exception.BadRequestException;
import ru.car.model.User;
import ru.car.repository.FirebaseTokenRepository;
import ru.car.service.message.MessageService;
import ru.car.service.security.AuthService;
import ru.car.service.security.JwtService;
import ru.car.service.security.SecurityUserService;

@Component
@RequiredArgsConstructor
public class LoginAuthMobileService {
    @Value("${admin.phone}")
    private String adminPhone;
    @Value("${admin.code}")
    private String adminCode;
    private final MessageService messageService;
    private final AuthenticationCodeService authenticationCodeService;
    private final UserService userService;
    private final JwtService jwtService;
    private final SecurityUserService securityUserService;
    private final FirebaseTokenRepository firebaseTokenRepository;
    private final AuthService authService;



    @Transactional
    public LoginAuthMobileRsParams loginMobile(LoginAuthMobileRqParams params) {
        String telephone = params.getPhoneNumber();
        if (!adminPhone.equals(telephone)) {
            if (authenticationCodeService.isAlreadySent(telephone)) {
                throw new BadRequestException("Code already sent to %s", ErrorCode.SMS_ALREADY_SENT, telephone);
            }
            String sendCode = messageService.sendFlashcallCode(telephone);
//            String sendCode = messageService.sendCallCode(telephone);
            authenticationCodeService.create(telephone, sendCode);
        }
        return LoginAuthMobileRsParams.builder()
                .timeToNextRequestSec(ApplicationConstants.SMS_NEXT_REQUEST_TIMEOUT_IN_SEC)
                .build();
    }

    @Transactional
    public LoginAuthCodeRsParams confirmCode(LoginAuthCodeRqParams params) {
        if (isAdmin(params) || authenticationCodeService.existsCode(params.getPhoneNumber(), params.getCode())) {
            authenticationCodeService.deleteAllByTelephone(params.getPhoneNumber());
            Role role = isAdmin(params) ? Role.ROLE_ADMIN : Role.ROLE_USER;
            User user = userService.findOrCreateByPhoneNumberAndActivate(params.getPhoneNumber(), role);
            return LoginAuthCodeRsParams.builder()
                    .token(jwtService.generateToken(securityUserService.getDetails(user)))
                    .build();
        }
        throw new BadRequestException(ErrorCode.EMPTY_CODE.getDescription(), ErrorCode.AUTH_MOBILE_CODE_NOT_RIGHT);
    }

    private boolean isAdmin(LoginAuthCodeRqParams params) {
        return adminPhone.equals(params.getPhoneNumber()) && adminCode.equals(params.getCode());
    }

    @Transactional
    public Void logout() {
        Long authId = authService.getAuthId();
        firebaseTokenRepository.deleteByAuthId(authId);
        return null;
    }
}
