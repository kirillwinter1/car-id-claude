package ru.car.repository;


import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.car.model.ReasonDictionary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReasonDictionaryRepository {
    private static final String EXISTS_BY_ID = "SELECT EXISTS(SELECT 1 FROM reason_dictionary rd WHERE rd.id = :id)";
    private static final String SELECT_BY_ID = "SELECT rd.* FROM reason_dictionary rd WHERE rd.id = :id";
    private static final String SELECT_ALL = "SELECT rd.* FROM reason_dictionary rd";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<ReasonDictionary> findAll() {
        return namedParameterJdbcTemplate.query(
                SELECT_ALL,
                BeanPropertyRowMapper.newInstance(ReasonDictionary.class)
        );
    }

    public Optional<ReasonDictionary> findById(Long id) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("id", id);
        return findOptionalNotification(SELECT_BY_ID, paramSource);
    }

    private Optional<ReasonDictionary> findOptionalNotification(String sql, SqlParameterSource paramSource) {
        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    sql,
                    paramSource,
                    BeanPropertyRowMapper.newInstance(ReasonDictionary.class)
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsById(Long id) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("id", id);
        return BooleanUtils.isTrue(
                namedParameterJdbcTemplate.queryForObject(EXISTS_BY_ID, paramSource, Boolean.class));
    }
}
