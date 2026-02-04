package ru.car.service.security;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import ru.car.enums.ErrorCode;
import ru.car.exception.UnauthorizedException;
import ru.car.model.User;
import ru.car.model.security.SecurityUser;
import ru.car.repository.UserRepository;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class SecurityUserService implements UserDetailsService {
    private final UserRepository userRepository;
    private SecureRandom random = new SecureRandom();

    @Override
    public SecurityUser loadUserByUsername(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNKNOWN_TOKEN.getDescription(), ErrorCode.UNKNOWN_TOKEN));
        if (BooleanUtils.isNotTrue(user.getActive())) {
            throw new UnauthorizedException(ErrorCode.UNKNOWN_TOKEN.getDescription(), ErrorCode.UNKNOWN_TOKEN);
        }
        return getDetails(user);
    }

    public SecurityUser loadUserByUsername(String phoneNumber, Long authId) {
        SecurityUser securityUser = loadUserByUsername(phoneNumber);
        securityUser.setAuthId(authId);
        return securityUser;
    }

    public SecurityUser getDetails(User user) {
        return SecurityUser.builder()
                .id(user.getId())
                .telephone(user.getPhoneNumber())
                .role(user.getRole())
                .authId(random.nextLong())
                .build();
    }
}
