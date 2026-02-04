package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.dto.FirebaseTokenDto;
import ru.car.model.FirebaseToken;
import ru.car.repository.FirebaseTokenRepository;
import ru.car.service.security.AuthService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FirebaseTokenService {

    private final FirebaseTokenRepository firebaseTokenRepository;
    private final AuthService authService;

    @Transactional
    public FirebaseTokenDto createOrUpdateToken(FirebaseTokenDto dto) {
        Long authId = authService.getAuthId();
        Long userId = authService.getUserId();

        Optional<FirebaseToken> tokenOpt = firebaseTokenRepository.findByAuthId(authId);

        if (tokenOpt.isPresent()) {
            FirebaseToken token = tokenOpt.get();
            token.setToken(dto.getToken());
            firebaseTokenRepository.updateTokenById(token);
        } else {
            FirebaseToken token = FirebaseToken.builder()
                    .authId(authId)
                    .userId(userId)
                    .token(dto.getToken())
                    .build();
            firebaseTokenRepository.save(token);
        }
        return dto;
    }
}
