package ru.car.repository;


import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.car.model.Batch;
import ru.car.model.Qr;

import java.sql.Types;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BatchRepository {

    private static final String SELECT_BY_ID = "SELECT b.* FROM qr_batches b join qrs q on b.id = q.batch_id WHERE q.id = :id";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Optional<Batch> findByQrId(UUID id) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("id", id);
        return findOptionalBatch(SELECT_BY_ID, paramSource);
    }

    private Optional<Batch> findOptionalBatch(String sql, SqlParameterSource paramSource) {
        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    sql,
                    paramSource,
                    BeanPropertyRowMapper.newInstance(Batch.class)
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private SqlParameterSource paramSource(Qr entity) {
        BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(entity);
        paramSource.registerSqlType("status", Types.VARCHAR);
        return paramSource;
    }
}
