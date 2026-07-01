package ru.car.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.car.dto.OwnerContactsDto;
import ru.car.dto.QrDto;
import ru.car.service.QrService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QrWebController Tests")
class QrWebControllerTest {

    @Mock
    QrService qrService;

    @InjectMocks
    QrWebController controller;

    @Test
    @DisplayName("GET qr/{id} использует UUID-перегрузку и отдаёт owner_contacts")
    void getQrForWeb_returnsOwnerContacts() {
        UUID id = UUID.randomUUID();
        // Стаб именно на UUID-перегрузку: если контроллер зовёт getQrById(QrDto) — стаб не сработает и тест упадёт.
        when(qrService.getQrById(id)).thenReturn(QrDto.builder()
                .qrId(id)
                .ownerContacts(OwnerContactsDto.builder().telegram("https://t.me/ivan").build())
                .build());

        QrDto body = controller.getQrForWeb(id).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getOwnerContacts()).isNotNull();
        assertThat(body.getOwnerContacts().getTelegram()).isEqualTo("https://t.me/ivan");
    }
}
