package ru.car.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class MonitoringRepository {
    private static final String INSERT_NEW = "INSERT INTO monitoring (event, user_id) VALUES (:event, :userId)";
    private static final String SELECT_COUNT_BY_EVENT_AND_AFTER = "SELECT count(*) FROM monitoring WHERE event = :event and created_date > :date ";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public int save(Events event, Long userId) {
        return namedParameterJdbcTemplate.update(
                INSERT_NEW,
                new MapSqlParameterSource()
                        .addValue("event", event.name())
                        .addValue("userId", userId)
        );
    }

    public int findCountByEventAndAfter(Events event, LocalDate date) {
        Integer result =  namedParameterJdbcTemplate.queryForObject(
                SELECT_COUNT_BY_EVENT_AND_AFTER,
                new MapSqlParameterSource()
                        .addValue("event", event.name())
                        .addValue("date", date),
                Integer.class
        );
        return Objects.isNull(result) ? 0 : result;
    }
}
