package ru.car.test.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.car.service.message.firebase.FirebaseService;

/**
 * Base class for repository integration tests with H2 database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseRepositoryTest {

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Mock FirebaseService to avoid loading Google credentials in tests.
     */
    @MockBean
    protected FirebaseService firebaseService;
}
