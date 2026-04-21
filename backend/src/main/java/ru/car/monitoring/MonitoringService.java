package ru.car.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.service.security.AuthService;

import java.util.concurrent.Callable;

@Component
@RequiredArgsConstructor
public class MonitoringService {
    private final MonitoringRepository monitoringRepository;
    private final AuthService authService;

    @Transactional
    public Object around(Events[] events, Callable<?> callable) throws Throwable {
        Long userId = authService.getUserIdOrNull();
        for (Events event: events) {
            monitoringRepository.save(event, userId);
        }
        return callable.call();
    }

    @Transactional
    public void monitor(Events event) {
        Long userId = authService.getUserIdOrNull();
        monitoringRepository.save(event, userId);
    }


}
