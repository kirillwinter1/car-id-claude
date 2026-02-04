package ru.car.repository;


import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.car.enums.QrStatus;
import ru.car.model.Qr;
import ru.car.util.KeyHolderUtils;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class QrRepository {
    private static final String INSERT_NEW_QR = "INSERT INTO qrs (id, name, batch_id, printed, status, user_id, created_date) VALUES (:id, :name, :batchId, :printed, :status, :userId, :createdDate)";
    private static final String SELECT_BY_ID_AND_USER = "SELECT q.* FROM qrs q WHERE q.id = :id and q.user_id = :userId";
    private static final String SELECT_BY_USER = "SELECT q.* FROM qrs q WHERE q.user_id = :userId ORDER BY q.status, q.activate_date DESC ";
    private static final String SELECT_BY_STATUS_AND_CREATED_DATE = "SELECT q.* FROM qrs q WHERE q.status = :status and q.created_date <= :createdDate";
    private static final String UPDATE_STATUS_AND_USER_AND_NAME = "UPDATE qrs SET user_id = :userId, status = :status, name = :name, updated_date = :updatedDate, activate_date = :activateDate WHERE id = :id";
    private static final String DELETE_BY_ID = "UPDATE qrs SET status = :status, delete_date = now() WHERE id = :id and status != 'TEMPORARY'";
    private static final String DESTROY_BY_ID = "DELETE FROM qrs q WHERE q.id = :id";
    private static final String DESTROY_BY_TEMPORARY_USER_ID = "DELETE FROM qrs WHERE user_id = :userId and status = 'TEMPORARY'";
    private static final String DELETE_BY_USER_ID = "UPDATE qrs SET status = :status, delete_date = now(), user_id = null WHERE user_id = :userId and status != 'TEMPORARY'";
    private static final String SELECT_BY_ID = "SELECT q.* FROM qrs q WHERE q.id = :id";
    private static final String EXISTS_BY_ID_AND_STATUS = "SELECT EXISTS(SELECT 1 FROM qrs q WHERE q.id = :id and q.status in (:status))";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

//    public List<Qr> findAll() {
//        return jdbcTemplate.query(
//                "SELECT * FROM qrs q left join users u ON q.owner_id = u.id",
//                new QrRowMapper()
//        );
//    }

    public List<Qr> findByUserId(Long id) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("userId", id);
        return namedParameterJdbcTemplate.query(SELECT_BY_USER, paramSource, BeanPropertyRowMapper.newInstance(Qr.class));
    }

    public List<Qr> findByStatusBefore(QrStatus status, LocalDateTime createdDate) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("status", status.name())
                .addValue("createdDate", createdDate);
        return namedParameterJdbcTemplate.query(SELECT_BY_STATUS_AND_CREATED_DATE, paramSource, BeanPropertyRowMapper.newInstance(Qr.class));
    }

    public Optional<Qr> findById(UUID id) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("id", id);
        return findOptionalQr(SELECT_BY_ID, paramSource);
    }

    public Optional<Qr> findByIdAndUser(UUID id, Long userId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("id", id)
                .addValue("userId", userId);
        return findOptionalQr(SELECT_BY_ID_AND_USER, paramSource);
    }

//    public boolean existsByIdAndActive(UUID id, Boolean active) {
//        String sql = "SELECT EXISTS(SELECT 1 FROM qrs WHERE id = ? and active = ?)";
//        return BooleanUtils.isTrue(
//                jdbcTemplate.queryForObject(sql,
//                        new Object[] { id, active },
//                        Boolean.class));
//    }
//
//    public boolean existsById(UUID id) {
//        String sql = "SELECT EXISTS(SELECT 1 FROM qrs WHERE id = ?)";
//        return BooleanUtils.isTrue(
//                jdbcTemplate.queryForObject(sql,
//                        new Object[] { id },
//                        Boolean.class));
//    }

    public Qr updateUserIdAndStatusAndName(Qr entity) {
        entity.setUpdatedDate(LocalDateTime.now());
        entity.setActivateDate(LocalDateTime.now());
        SqlParameterSource paramSource = paramSource(entity);
        namedParameterJdbcTemplate.update(UPDATE_STATUS_AND_USER_AND_NAME, paramSource);
        return entity;
    }

    public Qr delete(Qr entity) {
        entity.setDeleteDate(LocalDateTime.now());
        SqlParameterSource paramSource = paramSource(entity);
        namedParameterJdbcTemplate.update(DELETE_BY_ID, paramSource);
        return entity;
    }

    public boolean destroy(UUID id) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("id", id);
        return namedParameterJdbcTemplate.update(DESTROY_BY_ID, paramSource) == 1;
    }

    public boolean destroyTemporaryByUserId(Long userId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("userId", userId);
        return namedParameterJdbcTemplate.update(DESTROY_BY_TEMPORARY_USER_ID, paramSource) == 1;
    }

    public Qr save(Qr entity) {
        entity.setId(UUID.randomUUID());
        entity.setCreatedDate(LocalDateTime.now());
        SqlParameterSource paramSource = paramSource(entity);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(INSERT_NEW_QR, paramSource, keyHolder);
        entity.setSeqNumber(KeyHolderUtils.getQrSecNumber(keyHolder));
        return entity;
    }

    public void deleteAllByUserId(Long userId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource("status", QrStatus.DELETED.name())
                .addValue("userId", userId);
        namedParameterJdbcTemplate.update(DELETE_BY_USER_ID, paramSource);
    }

    private Optional<Qr> findOptionalQr(String sql, SqlParameterSource paramSource) {
        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    sql,
                    paramSource,
                    BeanPropertyRowMapper.newInstance(Qr.class)
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

    public boolean existsByIdAndStatus(UUID id, QrStatus ... status) {
        List<String> statuses = Arrays.stream(status)
                .map(s -> s.name())
                .collect(Collectors.toList());
        MapSqlParameterSource paramSource = new MapSqlParameterSource("status", statuses)
                .addValue("id", id);
        return BooleanUtils.isTrue(
                namedParameterJdbcTemplate.queryForObject(EXISTS_BY_ID_AND_STATUS, paramSource, Boolean.class));
    }
}
