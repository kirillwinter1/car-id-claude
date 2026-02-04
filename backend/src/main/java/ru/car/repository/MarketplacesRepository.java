package ru.car.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.car.model.Marketplaces;

@Repository
@RequiredArgsConstructor
public class MarketplacesRepository {
    private static final String SELECT_BY_ID = "SELECT m.* FROM marketplaces m WHERE m.id = 1";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Marketplaces findFirst() {
        return namedParameterJdbcTemplate.queryForObject(
                SELECT_BY_ID,
                new MapSqlParameterSource(),
                BeanPropertyRowMapper.newInstance(Marketplaces.class)
        );
    }
}
