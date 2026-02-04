package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.dto.NotificationSettingDto;
import ru.car.dto.UserDto;
import ru.car.enums.Role;
import ru.car.exception.NotFoundException;
import ru.car.mapper.UserDtoMapper;
import ru.car.model.User;
import ru.car.repository.FirebaseTokenRepository;
import ru.car.repository.UserRepository;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final NotificationSettingService notificationSettingService;
    private final QrService qrService;
    private final UserDtoMapper userDtoMapper;
    private final FirebaseTokenRepository firebaseTokenRepository;
    private final MetricService metricService;

    @Transactional
    public User findOrCreateByPhoneNumberAndActivate(String phoneNumber, Role role) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElse(null);
        if (Objects.isNull(user)) {
            user = userRepository.save(User.builder()
                    .role(role)
                    .phoneNumber(phoneNumber)
                    .active(true)
                    .build());
            notificationSettingService.create(user.getId());

            metricService.register();
        } else if (BooleanUtils.isNotTrue(user.getActive())) {
            user.setActive(true);
            user = userRepository.update(user);
            notificationSettingService.patch(user.getId(), NotificationSettingDto.builder()
                    .active(true)
                    .build());
        }
        return user;
    }

    public User getUserOrThrowNotFound(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public UserDto getUser(Long userId) {
        return userDtoMapper.toDto(getUserOrThrowNotFound(userId));
    }
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    @Transactional
    public UserDto deleteUser(Long userId) {
        User user = getUserOrThrowNotFound(userId);
        user.setActive(false);
        user = userRepository.update(user);
        notificationSettingService.deleteByUserId(userId);
        qrService.deleteAllByUserId(userId);
        firebaseTokenRepository.deleteAllByUserId(userId);
        return userDtoMapper.toDto(user);
    }
}
