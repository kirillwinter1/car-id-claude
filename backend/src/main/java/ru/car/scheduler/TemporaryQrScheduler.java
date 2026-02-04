package ru.car.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.car.service.QrService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
@RequiredArgsConstructor
public class TemporaryQrScheduler {
    private final QrService qrService;

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.HOURS)
    void deleteTemporaryQr() {
        log.debug("запуск процедуры удаление временных qr");
        try {
            List<UUID> uuids = qrService.destroyAllTemporaryQr();
            log.debug("удалены qr коды {}", uuids);
        } catch (Throwable t) {
            log.error("ошибка при запуске процедуры удаления временных qr", t);
        }
    }
}
