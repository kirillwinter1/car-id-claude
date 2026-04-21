package ru.car.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.car.dto.QrDto;
import ru.car.enums.QrStatus;
import ru.car.exception.ForbiddenException;
import ru.car.exception.NotFoundException;
import ru.car.mapper.QrDtoMapper;
import ru.car.mapper.QrWebDtoMapper;
import ru.car.model.Qr;
import ru.car.repository.NotificationRepository;
import ru.car.repository.QrRepository;
import ru.car.service.security.AuthService;
import ru.car.test.base.BaseUnitTest;
import ru.car.test.builder.QrBuilder;
import ru.car.test.util.SecurityTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("QrService Tests")
class QrServiceTest extends BaseUnitTest {

    @Mock
    private QrRepository qrRepository;

    @Mock
    private QrDtoMapper qrDtoMapper;

    @Mock
    private QrWebDtoMapper qrWebDtoMapper;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuthService authService;

    @Mock
    private MetricService metricService;

    @InjectMocks
    private QrService qrService;

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("should return QR when found")
        void shouldReturnQrWhenFound() {
            UUID qrId = UUID.randomUUID();
            Qr expectedQr = QrBuilder.aQr().withId(qrId).build();
            when(qrRepository.findById(qrId)).thenReturn(Optional.of(expectedQr));

            Qr result = qrService.findByIdOrThrowNotFound(qrId);

            assertThat(result).isEqualTo(expectedQr);
            verify(qrRepository).findById(qrId);
        }

        @Test
        @DisplayName("should throw NotFoundException when QR not found")
        void shouldThrowNotFoundExceptionWhenNotFound() {
            UUID qrId = UUID.randomUUID();
            when(qrRepository.findById(qrId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> qrService.findByIdOrThrowNotFound(qrId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findActiveQrById")
    class FindActiveQrById {

        @Test
        @DisplayName("should return QR when ACTIVE status exists")
        void shouldReturnQrWhenActiveStatusExists() {
            UUID qrId = UUID.randomUUID();
            Qr activeQr = QrBuilder.aQr().withId(qrId).asActive(1L).build();
            when(qrRepository.existsByIdAndStatus(qrId, QrStatus.ACTIVE, QrStatus.TEMPORARY)).thenReturn(true);
            when(qrRepository.findById(qrId)).thenReturn(Optional.of(activeQr));

            Qr result = qrService.findActiveQrById(qrId);

            assertThat(result).isEqualTo(activeQr);
        }

        @Test
        @DisplayName("should throw NotFoundException when QR is not ACTIVE or TEMPORARY")
        void shouldThrowNotFoundExceptionWhenNotActive() {
            UUID qrId = UUID.randomUUID();
            when(qrRepository.existsByIdAndStatus(qrId, QrStatus.ACTIVE, QrStatus.TEMPORARY)).thenReturn(false);

            assertThatThrownBy(() -> qrService.findActiveQrById(qrId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createQr")
    class CreateQr {

        @Test
        @DisplayName("should create QR with NEW status")
        void shouldCreateQrWithNewStatus() {
            QrDto expectedDto = QrDto.builder().build();
            when(qrRepository.save(any(Qr.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(qrDtoMapper.toDto(any(Qr.class))).thenReturn(expectedDto);

            QrDto result = qrService.createQr();

            ArgumentCaptor<Qr> qrCaptor = ArgumentCaptor.forClass(Qr.class);
            verify(qrRepository).save(qrCaptor.capture());
            Qr savedQr = qrCaptor.getValue();

            assertThat(savedQr.getStatus()).isEqualTo(QrStatus.NEW);
            assertThat(savedQr.getBatchId()).isEqualTo(1L);
            assertThat(savedQr.getPrinted()).isFalse();
            assertThat(result).isEqualTo(expectedDto);
        }
    }

    @Nested
    @DisplayName("createTemporaryQr")
    class CreateTemporaryQr {

        @Test
        @DisplayName("should create TEMPORARY QR for user without QR codes")
        void shouldCreateTemporaryQrForUserWithoutQrCodes() {
            Long userId = 1L;
            QrDto expectedDto = QrDto.builder().build();
            when(qrRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(qrRepository.save(any(Qr.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(qrDtoMapper.toDto(any(Qr.class))).thenReturn(expectedDto);

            QrDto result = qrService.createTemporaryQr(userId);

            ArgumentCaptor<Qr> qrCaptor = ArgumentCaptor.forClass(Qr.class);
            verify(qrRepository).save(qrCaptor.capture());
            Qr savedQr = qrCaptor.getValue();

            assertThat(savedQr.getStatus()).isEqualTo(QrStatus.TEMPORARY);
            assertThat(savedQr.getUserId()).isEqualTo(userId);
            assertThat(savedQr.getName()).isEqualTo("Временный qr");
            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        @DisplayName("should throw exception when user already has QR codes")
        void shouldThrowExceptionWhenUserAlreadyHasQrCodes() {
            Long userId = 1L;
            Qr existingQr = QrBuilder.aQr().asActive(userId).build();
            when(qrRepository.findByUserId(userId)).thenReturn(List.of(existingQr));

            assertThatThrownBy(() -> qrService.createTemporaryQr(userId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("linkToUser")
    class LinkToUser {

        @Test
        @DisplayName("should link NEW QR to user and set status to ACTIVE")
        void shouldLinkNewQrToUserAndSetActiveStatus() {
            Long userId = 1L;
            UUID qrId = UUID.randomUUID();
            Qr newQr = QrBuilder.aQr().withId(qrId).asNew().build();
            QrDto inputDto = QrDto.builder().qrId(qrId).qrName("My Car").build();
            QrDto expectedDto = QrDto.builder().build();

            when(authService.getUserId()).thenReturn(userId);
            when(qrRepository.findById(qrId)).thenReturn(Optional.of(newQr));
            when(qrRepository.updateUserIdAndStatusAndName(any(Qr.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(qrDtoMapper.toDto(any(Qr.class))).thenReturn(expectedDto);

            QrDto result = qrService.linkToUser(inputDto);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(newQr.getStatus()).isEqualTo(QrStatus.ACTIVE);
            assertThat(newQr.getUserId()).isEqualTo(userId);
            assertThat(newQr.getName()).isEqualTo("My Car");
            verify(metricService).activateQr();
        }

        @Test
        @DisplayName("should throw exception when QR is already ACTIVE")
        void shouldThrowExceptionWhenQrIsAlreadyActive() {
            Long userId = 1L;
            UUID qrId = UUID.randomUUID();
            Qr activeQr = QrBuilder.aQr().withId(qrId).asActive(2L).build();
            QrDto inputDto = QrDto.builder().qrId(qrId).qrName("My Car").build();

            when(authService.getUserId()).thenReturn(userId);
            when(qrRepository.findById(qrId)).thenReturn(Optional.of(activeQr));

            assertThatThrownBy(() -> qrService.linkToUser(inputDto))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when QR is TEMPORARY")
        void shouldThrowForbiddenExceptionWhenQrIsTemporary() {
            Long userId = 1L;
            UUID qrId = UUID.randomUUID();
            Qr temporaryQr = QrBuilder.aQr().withId(qrId).asTemporary(2L).build();
            QrDto inputDto = QrDto.builder().qrId(qrId).qrName("My Car").build();

            when(authService.getUserId()).thenReturn(userId);
            when(qrRepository.findById(qrId)).thenReturn(Optional.of(temporaryQr));

            assertThatThrownBy(() -> qrService.linkToUser(inputDto))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should not call metricService when linking non-NEW QR")
        void shouldNotCallMetricServiceWhenLinkingNonNewQr() {
            Long userId = 1L;
            UUID qrId = UUID.randomUUID();
            Qr deletedQr = QrBuilder.aQr().withId(qrId).asDeleted().build();
            QrDto inputDto = QrDto.builder().qrId(qrId).qrName("My Car").build();
            QrDto expectedDto = QrDto.builder().build();

            when(authService.getUserId()).thenReturn(userId);
            when(qrRepository.findById(qrId)).thenReturn(Optional.of(deletedQr));
            when(qrRepository.updateUserIdAndStatusAndName(any(Qr.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(qrDtoMapper.toDto(any(Qr.class))).thenReturn(expectedDto);

            qrService.linkToUser(inputDto);

            verify(metricService, never()).activateQr();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete QR and notifications")
        void shouldDeleteQrAndNotifications() {
            Long userId = 1L;
            UUID qrId = UUID.randomUUID();
            Qr activeQr = QrBuilder.aQr().withId(qrId).asActive(userId).build();
            QrDto inputDto = QrDto.builder().qrId(qrId).build();
            QrDto expectedDto = QrDto.builder().build();

            when(authService.getUserId()).thenReturn(userId);
            when(qrRepository.findByIdAndUser(qrId, userId)).thenReturn(Optional.of(activeQr));
            when(qrRepository.delete(any(Qr.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(qrDtoMapper.toDto(any(Qr.class))).thenReturn(expectedDto);

            QrDto result = qrService.delete(inputDto);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(activeQr.getStatus()).isEqualTo(QrStatus.DELETED);
            verify(qrRepository).delete(activeQr);
            verify(notificationRepository).deleteAllByUserId(userId);
        }

        @Test
        @DisplayName("should throw ForbiddenException when deleting TEMPORARY QR")
        void shouldThrowForbiddenExceptionWhenDeletingTemporaryQr() {
            Long userId = 1L;
            UUID qrId = UUID.randomUUID();
            Qr temporaryQr = QrBuilder.aQr().withId(qrId).asTemporary(userId).build();
            QrDto inputDto = QrDto.builder().qrId(qrId).build();

            when(authService.getUserId()).thenReturn(userId);
            when(qrRepository.findByIdAndUser(qrId, userId)).thenReturn(Optional.of(temporaryQr));

            assertThatThrownBy(() -> qrService.delete(inputDto))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw NotFoundException when QR not found for user")
        void shouldThrowNotFoundExceptionWhenQrNotFoundForUser() {
            Long userId = 1L;
            UUID qrId = UUID.randomUUID();
            QrDto inputDto = QrDto.builder().qrId(qrId).build();

            when(authService.getUserId()).thenReturn(userId);
            when(qrRepository.findByIdAndUser(qrId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> qrService.delete(inputDto))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("destroyAllTemporaryQr")
    class DestroyAllTemporaryQr {

        @Test
        @DisplayName("should destroy temporary QRs older than 1 hour")
        void shouldDestroyTemporaryQrsOlderThanOneHour() {
            UUID qrId1 = UUID.randomUUID();
            UUID qrId2 = UUID.randomUUID();
            Qr oldTemporaryQr1 = QrBuilder.aQr()
                    .withId(qrId1)
                    .asTemporary(1L)
                    .createdHoursAgo(2)
                    .build();
            Qr oldTemporaryQr2 = QrBuilder.aQr()
                    .withId(qrId2)
                    .asTemporary(2L)
                    .createdHoursAgo(3)
                    .build();

            when(qrRepository.findByStatusBefore(eq(QrStatus.TEMPORARY), any(LocalDateTime.class)))
                    .thenReturn(List.of(oldTemporaryQr1, oldTemporaryQr2));
            when(notificationRepository.deleteByQrId(any(UUID.class))).thenReturn(true);
            when(qrRepository.destroy(any(UUID.class))).thenReturn(true);

            List<UUID> result = qrService.destroyAllTemporaryQr();

            assertThat(result).containsExactlyInAnyOrder(qrId1, qrId2);
            verify(notificationRepository).deleteByQrId(qrId1);
            verify(notificationRepository).deleteByQrId(qrId2);
            verify(qrRepository).destroy(qrId1);
            verify(qrRepository).destroy(qrId2);
        }

        @Test
        @DisplayName("should return empty list when no temporary QRs to destroy")
        void shouldReturnEmptyListWhenNoTemporaryQrsToDestroy() {
            when(qrRepository.findByStatusBefore(eq(QrStatus.TEMPORARY), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            List<UUID> result = qrService.destroyAllTemporaryQr();

            assertThat(result).isEmpty();
            verify(notificationRepository, never()).deleteByQrId(any(UUID.class));
            verify(qrRepository, never()).destroy(any(UUID.class));
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("should return all QRs for authenticated user")
        void shouldReturnAllQrsForAuthenticatedUser() {
            Long userId = 1L;
            List<Qr> qrs = List.of(
                    QrBuilder.aQr().asActive(userId).build(),
                    QrBuilder.aQr().asActive(userId).build()
            );
            List<QrDto> expectedDtos = List.of(
                    QrDto.builder().build(),
                    QrDto.builder().build()
            );

            when(authService.getUserId()).thenReturn(userId);
            when(qrRepository.findByUserId(userId)).thenReturn(qrs);
            when(qrDtoMapper.toDto(qrs)).thenReturn(expectedDtos);

            List<QrDto> result = qrService.getAll();

            assertThat(result).isEqualTo(expectedDtos);
            verify(authService).getUserId();
            verify(qrRepository).findByUserId(userId);
        }
    }
}
