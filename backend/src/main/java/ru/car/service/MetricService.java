package ru.car.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.car.monitoring.Events;
import ru.car.monitoring.MonitoringService;

@Component
@RequiredArgsConstructor
public class MetricService {
    private final MeterRegistry meterRegistry;
    private final MonitoringService monitoringService;
    private Counter registerUser;
    private Counter activateQr;
    private Counter sendNotification;
    private Counter readNotification;

    @PostConstruct
    private void initOrderCounters() {
        registerUser = Counter.builder("car.register_user")
                .tag("type", "app")
                .description("The number of registration new user")
                .baseUnit("count")
                .register(meterRegistry);
        activateQr = Counter.builder("car.activate_qr")
                .tag("type", "app")
                .description("The number of activated qr")
                .baseUnit("count")
                .register(meterRegistry);
        sendNotification = Counter.builder("car.send_notification")
                .tag("type", "app")
                .description("The number of send notification")
                .baseUnit("count")
                .register(meterRegistry);
        readNotification = Counter.builder("car.read_notification")
                .tag("type", "app")
                .description("The number of read notification")
                .baseUnit("count")
                .register(meterRegistry);
    }

    public void register() {
        registerUser.increment();
        monitoringService.monitor(Events.REGISTER_USER);
    }

    public void sendNotification() {
        sendNotification.increment();
        monitoringService.monitor(Events.SEND_NOTIFICATION);
    }

    public void readNotification() {
        readNotification.increment();
        monitoringService.monitor(Events.READ_NOTIFICATION);
    }

    public void activateQr() {
        activateQr.increment();
        monitoringService.monitor(Events.ACTIVATE_QR);
    }
}
