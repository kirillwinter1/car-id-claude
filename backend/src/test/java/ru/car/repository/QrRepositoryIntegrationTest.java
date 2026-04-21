package ru.car.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import ru.car.enums.QrStatus;
import ru.car.model.Qr;
import ru.car.test.base.BaseRepositoryTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QrRepository Integration Tests")
class QrRepositoryIntegrationTest extends BaseRepositoryTest {

    @Autowired
    private QrRepository qrRepository;

    private Long testUserId1;
    private Long testUserId2;

    @BeforeEach
    void setUpTestUsers() {
        // Create test users for FK constraints
        testUserId1 = createTestUser("79001234567");
        testUserId2 = createTestUser("79009876543");
    }

    private Long createTestUser(String phone) {
        jdbcTemplate.update(
                "INSERT INTO users (phone_number, role, active) VALUES (:phone, 'ROLE_USER', true)",
                new MapSqlParameterSource("phone", phone)
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE phone_number = :phone",
                new MapSqlParameterSource("phone", phone),
                Long.class
        );
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should save QR with generated UUID")
        void shouldSaveQrWithGeneratedUuid() {
            Qr qr = Qr.builder()
                    .batchId(1L)
                    .name("Test QR")
                    .printed(false)
                    .status(QrStatus.NEW)
                    .build();

            Qr savedQr = qrRepository.save(qr);

            assertThat(savedQr.getId()).isNotNull();
            assertThat(savedQr.getCreatedDate()).isNotNull();
            assertThat(savedQr.getStatus()).isEqualTo(QrStatus.NEW);
        }

        @Test
        @DisplayName("should save QR with all fields")
        void shouldSaveQrWithAllFields() {
            Qr qr = Qr.builder()
                    .batchId(1L)
                    .name("My Car QR")
                    .printed(true)
                    .status(QrStatus.ACTIVE)
                    .userId(testUserId1)
                    .build();

            Qr savedQr = qrRepository.save(qr);

            assertThat(savedQr.getBatchId()).isEqualTo(1L);
            assertThat(savedQr.getName()).isEqualTo("My Car QR");
            assertThat(savedQr.getPrinted()).isTrue();
            assertThat(savedQr.getStatus()).isEqualTo(QrStatus.ACTIVE);
            assertThat(savedQr.getUserId()).isEqualTo(testUserId1);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should find QR by ID")
        void shouldFindQrById() {
            Qr savedQr = qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("Test QR")
                    .printed(false)
                    .status(QrStatus.NEW)
                    .build());

            Optional<Qr> foundQr = qrRepository.findById(savedQr.getId());

            assertThat(foundQr).isPresent();
            assertThat(foundQr.get().getId()).isEqualTo(savedQr.getId());
            assertThat(foundQr.get().getName()).isEqualTo("Test QR");
        }

        @Test
        @DisplayName("should return empty when QR not found")
        void shouldReturnEmptyWhenQrNotFound() {
            UUID nonExistentId = UUID.randomUUID();

            Optional<Qr> foundQr = qrRepository.findById(nonExistentId);

            assertThat(foundQr).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        @Test
        @DisplayName("should find all QRs by user ID")
        void shouldFindAllQrsByUserId() {
            qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("QR 1")
                    .printed(false)
                    .status(QrStatus.ACTIVE)
                    .userId(testUserId1)
                    .build());
            qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("QR 2")
                    .printed(false)
                    .status(QrStatus.ACTIVE)
                    .userId(testUserId1)
                    .build());
            qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("QR Other User")
                    .printed(false)
                    .status(QrStatus.ACTIVE)
                    .userId(testUserId2)
                    .build());

            List<Qr> userQrs = qrRepository.findByUserId(testUserId1);

            assertThat(userQrs).hasSize(2);
            assertThat(userQrs).allMatch(qr -> qr.getUserId().equals(testUserId1));
        }

        @Test
        @DisplayName("should return empty list when user has no QRs")
        void shouldReturnEmptyListWhenUserHasNoQrs() {
            Long userId = 999L;

            List<Qr> userQrs = qrRepository.findByUserId(userId);

            assertThat(userQrs).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatusBefore")
    class FindByStatusBefore {

        @Test
        @DisplayName("should find TEMPORARY QRs created before specified date")
        void shouldFindTemporaryQrsCreatedBeforeDate() {
            // Create QRs directly using JDBC to control createdDate
            Qr oldTemporaryQr = qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("Old Temporary")
                    .printed(false)
                    .status(QrStatus.TEMPORARY)
                    .userId(testUserId1)
                    .build());

            // Update createdDate to simulate old QR
            jdbcTemplate.update(
                    "UPDATE qrs SET created_date = :createdDate WHERE id = :id",
                    new MapSqlParameterSource()
                            .addValue("createdDate", LocalDateTime.now().minusHours(2))
                            .addValue("id", oldTemporaryQr.getId())
            );

            Qr recentTemporaryQr = qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("Recent Temporary")
                    .printed(false)
                    .status(QrStatus.TEMPORARY)
                    .userId(testUserId2)
                    .build());

            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            List<Qr> oldQrs = qrRepository.findByStatusBefore(QrStatus.TEMPORARY, oneHourAgo);

            assertThat(oldQrs).hasSize(1);
            assertThat(oldQrs.get(0).getId()).isEqualTo(oldTemporaryQr.getId());
        }
    }

    @Nested
    @DisplayName("existsByIdAndStatus")
    class ExistsByIdAndStatus {

        @Test
        @DisplayName("should return true when QR exists with specified status")
        void shouldReturnTrueWhenQrExistsWithStatus() {
            Qr activeQr = qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("Active QR")
                    .printed(false)
                    .status(QrStatus.ACTIVE)
                    .userId(testUserId1)
                    .build());

            boolean exists = qrRepository.existsByIdAndStatus(activeQr.getId(), QrStatus.ACTIVE);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when QR exists but with different status")
        void shouldReturnFalseWhenQrExistsWithDifferentStatus() {
            Qr newQr = qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("New QR")
                    .printed(false)
                    .status(QrStatus.NEW)
                    .build());

            boolean exists = qrRepository.existsByIdAndStatus(newQr.getId(), QrStatus.ACTIVE);

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should return true when checking multiple statuses")
        void shouldReturnTrueWhenCheckingMultipleStatuses() {
            Qr temporaryQr = qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("Temporary QR")
                    .printed(false)
                    .status(QrStatus.TEMPORARY)
                    .userId(testUserId1)
                    .build());

            boolean exists = qrRepository.existsByIdAndStatus(temporaryQr.getId(), QrStatus.ACTIVE, QrStatus.TEMPORARY);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when QR does not exist")
        void shouldReturnFalseWhenQrDoesNotExist() {
            UUID nonExistentId = UUID.randomUUID();

            boolean exists = qrRepository.existsByIdAndStatus(nonExistentId, QrStatus.ACTIVE);

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("updateUserIdAndStatusAndName")
    class UpdateUserIdAndStatusAndName {

        @Test
        @DisplayName("should update QR user, status and name")
        void shouldUpdateQrUserStatusAndName() {
            Qr qr = qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("Original Name")
                    .printed(false)
                    .status(QrStatus.NEW)
                    .build());

            qr.setUserId(testUserId1);
            qr.setStatus(QrStatus.ACTIVE);
            qr.setName("Updated Name");
            qrRepository.updateUserIdAndStatusAndName(qr);

            Optional<Qr> updatedQr = qrRepository.findById(qr.getId());

            assertThat(updatedQr).isPresent();
            assertThat(updatedQr.get().getUserId()).isEqualTo(testUserId1);
            assertThat(updatedQr.get().getStatus()).isEqualTo(QrStatus.ACTIVE);
            assertThat(updatedQr.get().getName()).isEqualTo("Updated Name");
            assertThat(updatedQr.get().getUpdatedDate()).isNotNull();
            assertThat(updatedQr.get().getActivateDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should mark QR as DELETED")
        void shouldMarkQrAsDeleted() {
            Qr qr = qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("Test QR")
                    .printed(false)
                    .status(QrStatus.ACTIVE)
                    .userId(testUserId1)
                    .build());

            qr.setStatus(QrStatus.DELETED);
            qrRepository.delete(qr);

            Optional<Qr> deletedQr = qrRepository.findById(qr.getId());

            assertThat(deletedQr).isPresent();
            assertThat(deletedQr.get().getStatus()).isEqualTo(QrStatus.DELETED);
        }
    }

    @Nested
    @DisplayName("destroy")
    class Destroy {

        @Test
        @DisplayName("should permanently delete QR")
        void shouldPermanentlyDeleteQr() {
            Qr qr = qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("Test QR")
                    .printed(false)
                    .status(QrStatus.TEMPORARY)
                    .userId(testUserId1)
                    .build());

            boolean destroyed = qrRepository.destroy(qr.getId());

            assertThat(destroyed).isTrue();
            assertThat(qrRepository.findById(qr.getId())).isEmpty();
        }

        @Test
        @DisplayName("should return false when QR does not exist")
        void shouldReturnFalseWhenQrDoesNotExist() {
            UUID nonExistentId = UUID.randomUUID();

            boolean destroyed = qrRepository.destroy(nonExistentId);

            assertThat(destroyed).isFalse();
        }
    }

    @Nested
    @DisplayName("findByIdAndUser")
    class FindByIdAndUser {

        @Test
        @DisplayName("should find QR by ID and user ID")
        void shouldFindQrByIdAndUserId() {
            Qr qr = qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("Test QR")
                    .printed(false)
                    .status(QrStatus.ACTIVE)
                    .userId(testUserId1)
                    .build());

            Optional<Qr> foundQr = qrRepository.findByIdAndUser(qr.getId(), testUserId1);

            assertThat(foundQr).isPresent();
            assertThat(foundQr.get().getId()).isEqualTo(qr.getId());
        }

        @Test
        @DisplayName("should return empty when QR belongs to different user")
        void shouldReturnEmptyWhenQrBelongsToDifferentUser() {
            Qr qr = qrRepository.save(Qr.builder()
                    .batchId(1L)
                    .name("Test QR")
                    .printed(false)
                    .status(QrStatus.ACTIVE)
                    .userId(testUserId1)
                    .build());

            Optional<Qr> foundQr = qrRepository.findByIdAndUser(qr.getId(), testUserId2);

            assertThat(foundQr).isEmpty();
        }
    }
}
