package ru.car.carId;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ru.car.service.message.firebase.FirebaseService;

@SpringBootTest
@ActiveProfiles("test")
class CarIdApplicationTests {

    @MockBean
    FirebaseService firebaseService;

    @Test
    void contextLoads() {
    }
}
