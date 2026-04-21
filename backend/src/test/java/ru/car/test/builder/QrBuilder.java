package ru.car.test.builder;

import ru.car.enums.QrStatus;
import ru.car.model.Qr;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builder for creating Qr instances in tests.
 */
public class QrBuilder {

    private UUID id = UUID.randomUUID();
    private Long seqNumber = 1L;
    private Long batchId = 1L;
    private String name = "Test QR";
    private Boolean printed = false;
    private QrStatus status = QrStatus.NEW;
    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime updatedDate = null;
    private LocalDateTime activateDate = null;
    private LocalDateTime deleteDate = null;
    private Long userId = null;

    public static QrBuilder aQr() {
        return new QrBuilder();
    }

    public QrBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public QrBuilder withSeqNumber(Long seqNumber) {
        this.seqNumber = seqNumber;
        return this;
    }

    public QrBuilder withBatchId(Long batchId) {
        this.batchId = batchId;
        return this;
    }

    public QrBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public QrBuilder withPrinted(Boolean printed) {
        this.printed = printed;
        return this;
    }

    public QrBuilder withStatus(QrStatus status) {
        this.status = status;
        return this;
    }

    public QrBuilder withCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public QrBuilder withUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
        return this;
    }

    public QrBuilder withActivateDate(LocalDateTime activateDate) {
        this.activateDate = activateDate;
        return this;
    }

    public QrBuilder withDeleteDate(LocalDateTime deleteDate) {
        this.deleteDate = deleteDate;
        return this;
    }

    public QrBuilder withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public QrBuilder asNew() {
        this.status = QrStatus.NEW;
        this.userId = null;
        return this;
    }

    public QrBuilder asActive(Long userId) {
        this.status = QrStatus.ACTIVE;
        this.userId = userId;
        this.activateDate = LocalDateTime.now();
        return this;
    }

    public QrBuilder asTemporary(Long userId) {
        this.status = QrStatus.TEMPORARY;
        this.userId = userId;
        this.name = "Временный qr";
        return this;
    }

    public QrBuilder asDeleted() {
        this.status = QrStatus.DELETED;
        this.deleteDate = LocalDateTime.now();
        return this;
    }

    public QrBuilder createdHoursAgo(int hours) {
        this.createdDate = LocalDateTime.now().minusHours(hours);
        return this;
    }

    public Qr build() {
        return Qr.builder()
                .id(id)
                .seqNumber(seqNumber)
                .batchId(batchId)
                .name(name)
                .printed(printed)
                .status(status)
                .createdDate(createdDate)
                .updatedDate(updatedDate)
                .activateDate(activateDate)
                .deleteDate(deleteDate)
                .userId(userId)
                .build();
    }
}
