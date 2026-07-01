package ru.car.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.car.dto.QrDto;
import ru.car.mapper.QrDtoMapper;
import ru.car.mapper.QrWebDtoMapper;
import ru.car.model.NotificationSetting;
import ru.car.model.Qr;
import ru.car.model.User;
import ru.car.repository.NotificationRepository;
import ru.car.repository.NotificationSettingRepository;
import ru.car.repository.QrRepository;
import ru.car.repository.UserRepository;
import ru.car.service.security.AuthService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QrService owner_contacts (BF6)")
class QrServiceOwnerContactsTest {

    @Mock QrRepository qrRepository;
    @Mock QrDtoMapper qrDtoMapper;
    @Mock QrWebDtoMapper qrWebDtoMapper;
    @Mock NotificationRepository notificationRepository;
    @Mock AuthService authService;
    @Mock MetricService metricService;
    @Mock NotificationSettingRepository notificationSettingRepository;
    @Mock UserRepository userRepository;

    @InjectMocks QrService qrService;

    @Test
    @DisplayName("отдаёт опубликованные контакты владельца")
    void returnsPublishedContacts() {
        UUID qrId = UUID.randomUUID();
        Qr qr = Qr.builder().id(qrId).userId(7L).build();
        when(qrRepository.findById(qrId)).thenReturn(Optional.of(qr));
        when(qrWebDtoMapper.toWebDto(qr)).thenReturn(QrDto.builder().qrId(qrId).build());
        when(notificationSettingRepository.findByQrId(qrId)).thenReturn(
                NotificationSetting.builder().userId(7L).showPhoneOnUnreachable(true)
                        .telegramContact("@ivan").build());
        when(userRepository.findById(7L)).thenReturn(Optional.of(
                User.builder().id(7L).phoneNumber("79991234567").build()));

        QrDto dto = qrService.getQrById(qrId);

        assertThat(dto.getOwnerContacts()).isNotNull();
        assertThat(dto.getOwnerContacts().getPhone()).isEqualTo("+79991234567");
        assertThat(dto.getOwnerContacts().getTelegram()).isEqualTo("https://t.me/ivan");
        assertThat(dto.getOwnerContacts().getVk()).isNull();
    }

    @Test
    @DisplayName("null-контакты, если владелец ничего не опубликовал")
    void nullWhenNothingPublished() {
        UUID qrId = UUID.randomUUID();
        Qr qr = Qr.builder().id(qrId).userId(7L).build();
        when(qrRepository.findById(qrId)).thenReturn(Optional.of(qr));
        when(qrWebDtoMapper.toWebDto(qr)).thenReturn(QrDto.builder().qrId(qrId).build());
        when(notificationSettingRepository.findByQrId(qrId)).thenReturn(
                NotificationSetting.builder().userId(7L).showPhoneOnUnreachable(false).build());

        assertThat(qrService.getQrById(qrId).getOwnerContacts()).isNull();
    }

    @Test
    @DisplayName("телефон OFF, но мессенджер задан → phone=null, telegram отдаётся")
    void phoneOffButMessengerSet() {
        UUID qrId = UUID.randomUUID();
        Qr qr = Qr.builder().id(qrId).userId(7L).build();
        when(qrRepository.findById(qrId)).thenReturn(Optional.of(qr));
        when(qrWebDtoMapper.toWebDto(qr)).thenReturn(QrDto.builder().qrId(qrId).build());
        when(notificationSettingRepository.findByQrId(qrId)).thenReturn(
                NotificationSetting.builder().userId(7L).active(true)
                        .showPhoneOnUnreachable(false).telegramContact("@ivan").build());

        var contacts = qrService.getQrById(qrId).getOwnerContacts();

        assertThat(contacts).isNotNull();
        assertThat(contacts.getPhone()).isNull();
        assertThat(contacts.getTelegram()).isEqualTo("https://t.me/ivan");
    }

    @Test
    @DisplayName("деактивированный аккаунт (active=false) → контакты не отдаём")
    void deactivatedAccountHidesContacts() {
        UUID qrId = UUID.randomUUID();
        Qr qr = Qr.builder().id(qrId).userId(7L).build();
        when(qrRepository.findById(qrId)).thenReturn(Optional.of(qr));
        when(qrWebDtoMapper.toWebDto(qr)).thenReturn(QrDto.builder().qrId(qrId).build());
        when(notificationSettingRepository.findByQrId(qrId)).thenReturn(
                NotificationSetting.builder().userId(7L).active(false)
                        .showPhoneOnUnreachable(true).telegramContact("@ivan").build());

        assertThat(qrService.getQrById(qrId).getOwnerContacts()).isNull();
    }
}
