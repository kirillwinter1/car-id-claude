package ru.car.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import ru.car.enums.QrStatus;
import ru.car.model.NotificationSetting;
import ru.car.model.Qr;
import ru.car.test.base.BaseRepositoryTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationSettingRepository Integration Tests")
class NotificationSettingRepositoryIntegrationTest extends BaseRepositoryTest {

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private QrRepository qrRepository;

    private Long userWithSettings;
    private Long userWithoutSettings;

    @BeforeEach
    void setUp() {
        userWithSettings = createTestUser("79001112233");
        userWithoutSettings = createTestUser("79004445566");
        notificationSettingRepository.save(NotificationSetting.builder()
                .userId(userWithSettings)
                .pushEnabled(true)
                .callEnabled(true)
                .telegramEnabled(false)
                .active(true)
                .showPhoneOnUnreachable(false)
                .build());
    }

    private Long createTestUser(String phone) {
        jdbcTemplate.update(
                "INSERT INTO users (phone_number, role, active) VALUES (:phone, 'ROLE_USER', true)",
                new MapSqlParameterSource("phone", phone));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE phone_number = :phone",
                new MapSqlParameterSource("phone", phone), Long.class);
    }

    private UUID createQrForUser(Long userId) {
        return qrRepository.save(Qr.builder()
                .batchId(1L)
                .name("QR")
                .printed(false)
                .status(QrStatus.ACTIVE)
                .userId(userId)
                .build()).getId();
    }

    @Nested
    @DisplayName("findByQrId")
    class FindByQrId {

        @Test
        @DisplayName("should return settings when QR owner has settings")
        void shouldReturnSettingsWhenOwnerHasSettings() {
            UUID qrId = createQrForUser(userWithSettings);

            NotificationSetting result = notificationSettingRepository.findByQrId(qrId);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userWithSettings);
            assertThat(result.getCallEnabled()).isTrue();
        }

        @Test
        @DisplayName("should return null when QR owner has no settings")
        void shouldReturnNullWhenOwnerHasNoSettings() {
            UUID qrId = createQrForUser(userWithoutSettings);

            NotificationSetting result = notificationSettingRepository.findByQrId(qrId);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when QR does not exist")
        void shouldReturnNullWhenQrDoesNotExist() {
            NotificationSetting result = notificationSettingRepository.findByQrId(UUID.randomUUID());

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("showPhoneOnUnreachable")
    class ShowPhoneOnUnreachable {

        @Test
        @DisplayName("сохраняется и обновляется через update")
        void savedAndUpdatable() {
            notificationSettingRepository.save(NotificationSetting.builder()
                    .userId(userWithoutSettings)
                    .pushEnabled(true)
                    .callEnabled(false)
                    .telegramEnabled(false)
                    .active(true)
                    .showPhoneOnUnreachable(false)
                    .build());

            NotificationSetting saved = notificationSettingRepository.findByUserId(userWithoutSettings);
            assertThat(saved.getShowPhoneOnUnreachable()).isFalse();

            saved.setShowPhoneOnUnreachable(true);
            notificationSettingRepository.update(saved);

            assertThat(notificationSettingRepository.findByUserId(userWithoutSettings).getShowPhoneOnUnreachable()).isTrue();
        }
    }

    @Nested
    @DisplayName("contacts (BF6)")
    class Contacts {

        @Test
        @DisplayName("update сохраняет telegram/vk/max contact")
        void updatePersistsContacts() {
            notificationSettingRepository.save(NotificationSetting.builder()
                    .userId(userWithoutSettings)
                    .pushEnabled(true)
                    .callEnabled(false)
                    .telegramEnabled(false)
                    .active(true)
                    .showPhoneOnUnreachable(false)
                    .build());

            NotificationSetting s = notificationSettingRepository.findByUserId(userWithoutSettings);
            s.setTelegramContact("@ivan");
            s.setVkContact("ivan_vk");
            s.setMaxContact("max.ru/u/abc");
            notificationSettingRepository.update(s);

            NotificationSetting r = notificationSettingRepository.findByUserId(userWithoutSettings);
            assertThat(r.getTelegramContact()).isEqualTo("@ivan");
            assertThat(r.getVkContact()).isEqualTo("ivan_vk");
            assertThat(r.getMaxContact()).isEqualTo("max.ru/u/abc");
        }
    }
}
