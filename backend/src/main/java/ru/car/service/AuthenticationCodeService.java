package ru.car.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.constants.ApplicationConstants;
import ru.car.model.AuthenticationCode;
import ru.car.repository.AuthenticationCodeRepository;
import ru.car.util.MessageUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationCodeService {
    private final AuthenticationCodeRepository repository;

    @Transactional
    public AuthenticationCode create(String phoneNumber, String code) {
        AuthenticationCode authenticationCode = AuthenticationCode.builder()
                .phoneNumber(phoneNumber)
                .code(code)
                .build();
        return repository.save(authenticationCode);
    }

    @Transactional
    public boolean isAlreadySent(String phoneNumber) {
        LocalDateTime date = LocalDateTime.of(LocalDate.now(), LocalTime.now().minusSeconds(ApplicationConstants.SMS_NEXT_REQUEST_TIMEOUT_IN_SEC));
        return repository.existsByPhoneNumberAndAfter(phoneNumber, date);
    }

    @Transactional
    public boolean deleteAllByTelephone(String telephone) {
        return repository.deleteAllByTelephone(telephone);
    }

    @Transactional
    public boolean existsCode(String telephone, String code) {
        return repository.existByTelephoneAndCode(telephone, code);
    }

    @Transactional
    public void createCodeFromCallerNumber(String phoneFrom, String phoneTo) {
        if (!MessageUtils.isValidPhone(phoneFrom) || !MessageUtils.isValidPhone(phoneFrom)) {
            return;
        }
        phoneFrom = MessageUtils.getValidPhone(phoneFrom);
        phoneTo = MessageUtils.getValidPhone(phoneTo);

        String code = phoneFrom.substring(phoneFrom.length() - 4);

        if (existsCode(phoneTo, code) ) {
            return;
        }

        AuthenticationCode authenticationCode = AuthenticationCode.builder()
                .phoneNumber(phoneTo)
                .code(code)
                .build();
        repository.save(authenticationCode);
    }
}
