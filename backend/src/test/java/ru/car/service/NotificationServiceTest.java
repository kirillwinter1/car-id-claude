package ru.car.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.car.dto.NotificationDto;
import ru.car.dto.PageParam;
import ru.car.enums.NotificationStatus;
import ru.car.enums.QrStatus;
import ru.car.exception.BadRequestException;
import ru.car.exception.ForbiddenException;
import ru.car.exception.NotFoundException;
import ru.car.mapper.NotificationDtoMapper;
import ru.car.model.Notification;
import ru.car.model.Qr;
import ru.car.model.ReasonDictionary;
import ru.car.repository.NotificationRepository;
import ru.car.service.security.AuthService;
import ru.car.test.base.BaseUnitTest;
import ru.car.test.builder.NotificationBuilder;
import ru.car.test.builder.QrBuilder;
import ru.car.test.util.SecurityTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("NotificationService Tests")
class NotificationServiceTest extends BaseUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDtoMapper notificationDtoMapper;

    @Mock
    private ReasonDictionaryService reasonDictionaryService;

    @Mock
    private QrService qrService;

    @Mock
    private AuthService authService;

    @Mock
    private MetricService metricService;

    @InjectMocks
    private NotificationService notificationService;

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("should return notification when found and not expired DRAFT")
        void shouldReturnNotificationWhenFoundAndNotExpiredDraft() {
            UUID notificationId = UUID.randomUUID();
            Notification notification = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .asDraft()
                    .withCreatedDate(LocalDateTime.now())
                    .build();
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

            Notification result = notificationService.findByIdOrThrowNotFound(notificationId);

            assertThat(result).isEqualTo(notification);
            verify(notificationRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should throw NotFoundException when notification not found")
        void shouldThrowNotFoundExceptionWhenNotFound() {
            UUID notificationId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.findByIdOrThrowNotFound(notificationId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("should delete and throw when DRAFT notification is older than 5 minutes")
        void shouldDeleteAndThrowWhenDraftIsOlderThan5Minutes() {
            UUID notificationId = UUID.randomUUID();
            Notification expiredDraft = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .asDraft()
                    .createdMinutesAgo(10)
                    .build();
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(expiredDraft));

            assertThatThrownBy(() -> notificationService.findByIdOrThrowNotFound(notificationId))
                    .isInstanceOf(NotFoundException.class);

            verify(notificationRepository).deleteById(notificationId);
        }

        @Test
        @DisplayName("should return UNREAD notification regardless of age")
        void shouldReturnUnreadNotificationRegardlessOfAge() {
            UUID notificationId = UUID.randomUUID();
            Notification oldUnread = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .asUnread()
                    .createdMinutesAgo(60)
                    .build();
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(oldUnread));

            Notification result = notificationService.findByIdOrThrowNotFound(notificationId);

            assertThat(result).isEqualTo(oldUnread);
            verify(notificationRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("createDraft")
    class CreateDraft {

        @Test
        @DisplayName("should create notification with DRAFT status")
        void shouldCreateNotificationWithDraftStatus() {
            UUID qrId = UUID.randomUUID();
            Long reasonId = 1L;
            Qr activeQr = QrBuilder.aQr().withId(qrId).asActive(1L).build();
            ReasonDictionary reason = ReasonDictionary.builder().id(reasonId).description("Test").build();
            NotificationDto inputDto = NotificationDto.builder()
                    .qrId(qrId)
                    .reasonId(reasonId)
                    .visitorId("visitor123")
                    .build();
            NotificationDto expectedDto = NotificationDto.builder().build();

            when(qrService.findActiveQrById(qrId)).thenReturn(activeQr);
            when(reasonDictionaryService.findByIdOrThrowNotFound(reasonId)).thenReturn(reason);
            when(authService.getUserIdOrNull()).thenReturn(null);
            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(notificationDtoMapper.toDto(any(Notification.class))).thenReturn(expectedDto);

            NotificationDto result = notificationService.createDraft(inputDto);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            Notification savedNotification = captor.getValue();

            assertThat(savedNotification.getStatus()).isEqualTo(NotificationStatus.DRAFT);
            assertThat(savedNotification.getQrId()).isEqualTo(qrId);
            assertThat(savedNotification.getReasonId()).isEqualTo(reasonId);
            assertThat(savedNotification.getVisitorId()).isEqualTo("visitor123");
            assertThat(result).isEqualTo(expectedDto);
        }
    }

    @Nested
    @DisplayName("createUnread")
    class CreateUnread {

        @Test
        @DisplayName("should create notification with UNREAD status")
        void shouldCreateNotificationWithUnreadStatus() {
            UUID qrId = UUID.randomUUID();
            Long reasonId = 1L;
            Long senderId = 2L;
            Qr activeQr = QrBuilder.aQr().withId(qrId).asActive(1L).build();
            ReasonDictionary reason = ReasonDictionary.builder().id(reasonId).description("Test").build();
            NotificationDto inputDto = NotificationDto.builder()
                    .qrId(qrId)
                    .reasonId(reasonId)
                    .build();
            NotificationDto expectedDto = NotificationDto.builder().build();

            when(qrService.findActiveQrById(qrId)).thenReturn(activeQr);
            when(reasonDictionaryService.findByIdOrThrowNotFound(reasonId)).thenReturn(reason);
            when(authService.getUserIdOrNull()).thenReturn(senderId);
            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(notificationDtoMapper.toDto(any(Notification.class))).thenReturn(expectedDto);

            NotificationDto result = notificationService.createUnread(inputDto);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            Notification savedNotification = captor.getValue();

            assertThat(savedNotification.getStatus()).isEqualTo(NotificationStatus.UNREAD);
            assertThat(savedNotification.getSenderId()).isEqualTo(senderId);
            assertThat(result).isEqualTo(expectedDto);
        }
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("should mark notification as READ")
        void shouldMarkNotificationAsRead() {
            Long userId = 1L;
            UUID notificationId = UUID.randomUUID();
            UUID qrId = UUID.randomUUID();
            Qr qr = QrBuilder.aQr().withId(qrId).asActive(userId).build();
            Notification unreadNotification = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .withQr(qr)
                    .asUnread()
                    .build();
            NotificationDto inputDto = NotificationDto.builder()
                    .notificationId(notificationId)
                    .build();
            NotificationDto expectedDto = NotificationDto.builder().build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(unreadNotification));
            when(notificationRepository.updateStatus(any(Notification.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(notificationDtoMapper.toDto(any(Notification.class))).thenReturn(expectedDto);

            NotificationDto result = notificationService.read(inputDto, userId);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(unreadNotification.getStatus()).isEqualTo(NotificationStatus.READ);
            verify(metricService).readNotification();
        }

        @Test
        @DisplayName("should throw ForbiddenException when user is not owner")
        void shouldThrowForbiddenExceptionWhenUserIsNotOwner() {
            Long userId = 1L;
            Long differentUserId = 2L;
            UUID notificationId = UUID.randomUUID();
            UUID qrId = UUID.randomUUID();
            Qr qr = QrBuilder.aQr().withId(qrId).asActive(differentUserId).build();
            Notification notification = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .withQr(qr)
                    .asUnread()
                    .build();
            NotificationDto inputDto = NotificationDto.builder()
                    .notificationId(notificationId)
                    .build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.read(inputDto, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when notification already READ")
        void shouldThrowForbiddenExceptionWhenAlreadyRead() {
            Long userId = 1L;
            UUID notificationId = UUID.randomUUID();
            UUID qrId = UUID.randomUUID();
            Qr qr = QrBuilder.aQr().withId(qrId).asActive(userId).build();
            Notification readNotification = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .withQr(qr)
                    .asRead()
                    .build();
            NotificationDto inputDto = NotificationDto.builder()
                    .notificationId(notificationId)
                    .build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(readNotification));

            assertThatThrownBy(() -> notificationService.read(inputDto, userId))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("updateDraft")
    class UpdateDraft {

        @Test
        @DisplayName("should update draft notification with new reason")
        void shouldUpdateDraftWithNewReason() {
            UUID notificationId = UUID.randomUUID();
            Long oldReasonId = 1L;
            Long newReasonId = 2L;
            ReasonDictionary newReason = ReasonDictionary.builder()
                    .id(newReasonId)
                    .description("New reason")
                    .build();
            Notification draft = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .withReasonId(oldReasonId)
                    .asDraft()
                    .build();
            NotificationDto inputDto = NotificationDto.builder()
                    .notificationId(notificationId)
                    .reasonId(newReasonId)
                    .build();
            NotificationDto expectedDto = NotificationDto.builder().build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(draft));
            when(reasonDictionaryService.findByIdOrThrowNotFound(newReasonId)).thenReturn(newReason);
            when(notificationRepository.updateReasonAndTextAndStatus(any(Notification.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(notificationDtoMapper.toDto(any(Notification.class))).thenReturn(expectedDto);

            NotificationDto result = notificationService.updateDraft(inputDto);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(draft.getReasonId()).isEqualTo(newReasonId);
            assertThat(draft.getReason()).isEqualTo(newReason);
        }

        @Test
        @DisplayName("should throw ForbiddenException when notification has senderId")
        void shouldThrowForbiddenExceptionWhenNotificationHasSenderId() {
            UUID notificationId = UUID.randomUUID();
            Notification notDraft = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .asDraft()
                    .withSenderId(1L)
                    .build();
            NotificationDto inputDto = NotificationDto.builder()
                    .notificationId(notificationId)
                    .build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notDraft));

            assertThatThrownBy(() -> notificationService.updateDraft(inputDto))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when notification status is not DRAFT")
        void shouldThrowForbiddenExceptionWhenStatusIsNotDraft() {
            UUID notificationId = UUID.randomUUID();
            Notification unreadNotification = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .asUnread()
                    .build();
            NotificationDto inputDto = NotificationDto.builder()
                    .notificationId(notificationId)
                    .build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(unreadNotification));

            assertThatThrownBy(() -> notificationService.updateDraft(inputDto))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException when rate limit exceeded (1 min between sends)")
        void shouldThrowBadRequestExceptionWhenRateLimitExceeded() {
            UUID notificationId = UUID.randomUUID();
            UUID qrId = UUID.randomUUID();
            Notification draft = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .withQrId(qrId)
                    .asDraft()
                    .build();
            Notification recentNotification = NotificationBuilder.aNotification()
                    .withCreatedDate(LocalDateTime.now().minusSeconds(30))
                    .build();
            NotificationDto inputDto = NotificationDto.builder()
                    .notificationId(notificationId)
                    .qrId(qrId)
                    .status(NotificationStatus.SEND)
                    .build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(draft));
            when(notificationRepository.findByQrIdAndDateAfter(eq(qrId), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(recentNotification));

            assertThatThrownBy(() -> notificationService.updateDraft(inputDto))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("should change status to UNREAD when sending draft")
        void shouldChangeStatusToUnreadWhenSendingDraft() {
            UUID notificationId = UUID.randomUUID();
            UUID qrId = UUID.randomUUID();
            ReasonDictionary reason = ReasonDictionary.builder().id(1L).description("Test").build();
            Notification draft = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .withQrId(qrId)
                    .withReason(reason)
                    .asDraft()
                    .build();
            NotificationDto inputDto = NotificationDto.builder()
                    .notificationId(notificationId)
                    .qrId(qrId)
                    .status(NotificationStatus.SEND)
                    .build();
            NotificationDto expectedDto = NotificationDto.builder().build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(draft));
            when(notificationRepository.findByQrIdAndDateAfter(eq(qrId), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());
            when(notificationRepository.updateReasonAndTextAndStatus(any(Notification.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(notificationDtoMapper.toDto(any(Notification.class))).thenReturn(expectedDto);

            NotificationDto result = notificationService.updateDraft(inputDto);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(draft.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        }

        @Test
        @DisplayName("should update text when different from reason description")
        void shouldUpdateTextWhenDifferentFromReasonDescription() {
            UUID notificationId = UUID.randomUUID();
            ReasonDictionary reason = ReasonDictionary.builder()
                    .id(1L)
                    .description("Original description")
                    .build();
            Notification draft = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .withReason(reason)
                    .asDraft()
                    .build();
            NotificationDto inputDto = NotificationDto.builder()
                    .notificationId(notificationId)
                    .text("Custom message")
                    .build();
            NotificationDto expectedDto = NotificationDto.builder().build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(draft));
            when(notificationRepository.updateReasonAndTextAndStatus(any(Notification.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(notificationDtoMapper.toDto(any(Notification.class))).thenReturn(expectedDto);

            notificationService.updateDraft(inputDto);

            assertThat(draft.getText()).isEqualTo("Custom message");
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("should return notification status")
        void shouldReturnNotificationStatus() {
            UUID notificationId = UUID.randomUUID();
            Notification notification = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .asUnread()
                    .build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

            var result = notificationService.getStatus(notificationId);

            assertThat(result.getNotificationId()).isEqualTo(notificationId);
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        }
    }

    @Nested
    @DisplayName("isUnread")
    class IsUnread {

        @Test
        @DisplayName("should return true for UNREAD notification")
        void shouldReturnTrueForUnreadNotification() {
            UUID notificationId = UUID.randomUUID();
            Notification unread = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .asUnread()
                    .build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(unread));

            boolean result = notificationService.isUnread(notificationId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for READ notification")
        void shouldReturnFalseForReadNotification() {
            UUID notificationId = UUID.randomUUID();
            Notification read = NotificationBuilder.aNotification()
                    .withId(notificationId)
                    .asRead()
                    .build();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(read));

            boolean result = notificationService.isUnread(notificationId);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("shouldShowMarkAsReadButton")
    class ShouldShowMarkAsReadButton {

        @Test
        @DisplayName("should return true for UNREAD notification")
        void shouldShowMarkAsReadButton_returnsTrue_forUnread() {
            UUID id = UUID.randomUUID();
            Notification notification = new Notification();
            notification.setStatus(NotificationStatus.UNREAD);
            when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

            assertThat(notificationService.shouldShowMarkAsReadButton(id)).isTrue();
        }

        @Test
        @DisplayName("should return false for READ notification")
        void shouldShowMarkAsReadButton_returnsFalse_forRead() {
            UUID id = UUID.randomUUID();
            Notification notification = new Notification();
            notification.setStatus(NotificationStatus.READ);
            when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

            assertThat(notificationService.shouldShowMarkAsReadButton(id)).isFalse();
        }

        @Test
        @DisplayName("should return true when notification not found (button shown)")
        void shouldShowMarkAsReadButton_returnsTrue_whenNotFound() {
            UUID id = UUID.randomUUID();
            when(notificationRepository.findById(id)).thenReturn(Optional.empty());

            // Preserves current TelegramBotService.readNotification behavior:
            // if notification is missing, status=null, null != READ → true (button shown).
            assertThat(notificationService.shouldShowMarkAsReadButton(id)).isTrue();
        }
    }

    @Nested
    @DisplayName("countUnreadByUserId")
    class CountUnreadByUserId {

        @Test
        @DisplayName("should delegate to repository and return count")
        void countUnreadByUserId_delegatesToRepo() {
            when(notificationRepository.findCountByUserIdAndStatus(1L, NotificationStatus.UNREAD))
                    .thenReturn(5);
            assertThat(notificationService.countUnreadByUserId(1L)).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete notification")
        void shouldDeleteNotification() {
            UUID notificationId = UUID.randomUUID();
            NotificationDto inputDto = NotificationDto.builder()
                    .notificationId(notificationId)
                    .build();

            when(notificationRepository.deleteById(notificationId)).thenReturn(true);

            boolean result = notificationService.delete(inputDto);

            assertThat(result).isTrue();
            verify(notificationRepository).deleteById(notificationId);
        }

        @Test
        @DisplayName("should return false when notification not found")
        void shouldReturnFalseWhenNotificationNotFound() {
            UUID notificationId = UUID.randomUUID();
            NotificationDto inputDto = NotificationDto.builder()
                    .notificationId(notificationId)
                    .build();

            when(notificationRepository.deleteById(notificationId)).thenReturn(false);

            boolean result = notificationService.delete(inputDto);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findForBot")
    class FindForBot {

        @Test
        @DisplayName("all tab, no qr filter: delegates to findPageByUserId")
        void findForBot_allTab_noQrFilter_delegatesToFindPageByUserId() {
            PageParam page = PageParam.builder().page(0).size(5).build();
            when(notificationRepository.findPageByUserId(42L, page))
                    .thenReturn(List.of(new Notification()));

            var result = notificationService.findForBot(42L, "all", null, page);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("unread tab, no qr filter: delegates to findPageByUserIdAndStatus")
        void findForBot_unreadTab_filtersByStatus() {
            PageParam page = PageParam.builder().page(0).size(5).build();
            when(notificationRepository.findPageByUserIdAndStatus(42L, NotificationStatus.UNREAD, page))
                    .thenReturn(List.of(new Notification()));

            var result = notificationService.findForBot(42L, "unread", null, page);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("all tab, with qr filter: delegates to findPageByUserIdAndQrId")
        void findForBot_withQrFilter_allTab_delegatesToByQrId() {
            PageParam page = PageParam.builder().page(0).size(5).build();
            UUID qrId = UUID.randomUUID();
            when(notificationRepository.findPageByUserIdAndQrId(42L, qrId, page))
                    .thenReturn(List.of(new Notification()));

            var result = notificationService.findForBot(42L, "all", qrId, page);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("unread tab, with qr filter: delegates to findPageByUserIdAndStatusAndQrId")
        void findForBot_withQrFilter_unreadTab_delegatesToByStatusAndQrId() {
            PageParam page = PageParam.builder().page(0).size(5).build();
            UUID qrId = UUID.randomUUID();
            when(notificationRepository.findPageByUserIdAndStatusAndQrId(42L, NotificationStatus.UNREAD, qrId, page))
                    .thenReturn(List.of(new Notification()));

            var result = notificationService.findForBot(42L, "unread", qrId, page);

            assertThat(result).hasSize(1);
        }
    }
}
